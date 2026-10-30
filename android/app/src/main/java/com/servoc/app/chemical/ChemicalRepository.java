package com.servoc.app.chemical;

import android.database.Cursor;
import org.json.JSONObject;
import org.json.JSONArray;
import com.servoc.app.data.ExperimentsDb;
import java.util.ArrayList;
import java.util.List;

/** Unified V12 chemical master catalog. 144 legacy indices are identity/provenance flags, NOT a separate selectable library. */
public final class ChemicalRepository {
    private final ExperimentsDb db;
    public ChemicalRepository(ExperimentsDb db){this.db=db;ChemicalIdentitySchema.ensure(db.getWritableDatabase());}
    public ExperimentsDb database(){return db;}

    public static final class Chemical {
        public final long uid;public final Integer legacyId,pubchemCid;public final String cas,name,arabicName,formula,chemicalClass,source;public final Double molarMass;
        Chemical(long uid,Integer legacyId,String cas,Integer pubchemCid,String name,String arabicName,String formula,Double molarMass,String chemicalClass,String source){this.uid=uid;this.legacyId=legacyId;this.cas=cas;this.pubchemCid=pubchemCid;this.name=name;this.arabicName=arabicName;this.formula=formula;this.molarMass=molarMass;this.chemicalClass=chemicalClass;this.source=source;}
        public String label(){StringBuilder s=new StringBuilder(name==null?"":name);if(cas!=null&&!cas.isEmpty())s.append(" • CAS ").append(cas);return s.toString();}
    }

    private static final String COLS="uid,legacy_id,cas,pubchem_cid,name_en,name_ar,formula,molar_mass,chemical_class,record_source";

    public int count(){
        try(Cursor c=db.getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM v14_unified_chemical_catalog",null)){
            return c.moveToFirst()?c.getInt(0):0;
        }
    }

    /** The only user-facing full list, with stable master UID and original legacy flag. */
    public List<Chemical> all(int limit){return search("",limit);}

    /** Current high-priority subset. It references the same master rows; no chemical facts are duplicated. */
    public List<Chemical> priority(int limit){
        ChemicalPriorityBootstrap.ensure(db.getWritableDatabase());
        int n=Math.max(1,Math.min(limit,5000));List<Chemical> out=new ArrayList<>();
        String sql="SELECT c."+COLS.replace(",",",c.")+" FROM chemicals c JOIN chemical_priority p ON p.chemical_uid=c.uid WHERE c.active=1 ORDER BY CASE p.priority_class WHEN 'BASE_144' THEN 0 ELSE 1 END, CASE WHEN c.legacy_id IS NULL THEN 999999 ELSE c.legacy_id END, lower(c.name_en) LIMIT "+n;
        try(Cursor c=db.getReadableDatabase().rawQuery(sql,null)){while(c.moveToNext())out.add(read(c));}
        return out;
    }

    public int priorityCount(){
        ChemicalPriorityBootstrap.ensure(db.getWritableDatabase());
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM chemical_priority p JOIN chemicals c ON c.uid=p.chemical_uid WHERE c.active=1",null)){return c.moveToFirst()?c.getInt(0):0;}
    }

    public boolean isPriority(long uid){
        ChemicalPriorityBootstrap.ensure(db.getWritableDatabase());
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT 1 FROM chemical_priority WHERE chemical_uid=? LIMIT 1",new String[]{String.valueOf(uid)})){return c.moveToFirst();}
    }

    public List<Chemical> search(String query,int limit){
        String q=query==null?"":query.trim();int n=Math.max(1,Math.min(limit,5000));List<Chemical> out=new ArrayList<>();
        String sql;String[] args;
        if(q.isEmpty()){sql="SELECT "+COLS+" FROM v14_unified_chemical_catalog ORDER BY name_en LIMIT "+n;args=null;}
        else{sql="SELECT DISTINCT c.uid,c.legacy_id,c.cas,c.pubchem_cid,c.name_en,c.name_ar,c.formula,c.molar_mass,c.chemical_class,c.record_source "+
            "FROM v14_unified_chemical_catalog c "+
            "LEFT JOIN chemical_synonyms s ON s.chemical_uid=c.uid "+
            "LEFT JOIN chemical_identifiers i ON i.chemical_uid=c.uid "+
            "LEFT JOIN chemical_identity_equivalence_v6 e ON e.alias_uid=c.uid "+
            "LEFT JOIN chemicals ni ON ni.uid=e.canonical_uid "+
            "WHERE (c.name_en LIKE ? OR c.name_ar LIKE ? OR c.cas LIKE ? "+
            "OR c.formula LIKE ? OR s.synonym LIKE ? OR i.value LIKE ? "+
            "OR e.source_cas LIKE ? OR ni.name_en LIKE ?) "+
            "ORDER BY c.name_en LIMIT "+n;
            String x="%"+q+"%";args=new String[]{x,x,x,x,x,x,x,x};}
        try(Cursor c=db.getReadableDatabase().rawQuery(sql,args)){while(c.moveToNext())out.add(read(c));}
        return out;
    }

    /**
     * Read-only authority CAS for a historical alias whose master row does
     * not itself have CAS. The value is NOT assigned to the original row.
     * Show the qualification label whenever a UI surfaces it.
     */
    public String sourceLinkedCas(long uid){
        try(Cursor c=db.getReadableDatabase().rawQuery(
                "SELECT e.source_cas FROM chemical_identity_equivalence_v6 e "+
                "JOIN chemical_sources src ON src.chemical_uid=e.canonical_uid "+
                "AND src.source_id=e.source_id AND src.record_url=e.source_url "+
                "WHERE e.alias_uid=? AND e.evidence_status='NIOSH_SYNONYM_VERIFIED' "+
                "LIMIT 1",new String[]{Long.toString(uid)})){
            return c.moveToFirst()?c.getString(0):null;
        }
    }

    /** No fake CAS for mixtures or unverified compounds; source is explicit. */
    public String catalogCasLabel(Chemical chemical){
        if(chemical==null)return "—";
        if(chemical.cas!=null&&!chemical.cas.trim().isEmpty())
            return chemical.cas;
        String qualified=sourceLinkedCas(chemical.uid);
        return qualified==null||qualified.isEmpty()?
            "—":"CAS مرجعي NIOSH: "+qualified;
    }

    /** Exact UID/CAS/name/synonym first; one unique fuzzy result is accepted as a convenience. */
    public Chemical resolve(String query){
        String q=query==null?"":query.trim();if(q.isEmpty())return null;
        if(q.matches("\\d+")){try{Chemical c=byUid(Long.parseLong(q));if(c!=null)return c;}catch(Exception ignored){}}
        Chemical c=exact(q);if(c!=null)return c;
        List<Chemical> rows=search(q,3);return rows.size()==1?rows.get(0):null;
    }

    public Chemical byLegacyId(int id){try(Cursor c=db.getReadableDatabase().rawQuery("SELECT "+COLS+" FROM v14_unified_chemical_catalog WHERE legacy_id=?",new String[]{String.valueOf(id)})){return c.moveToFirst()?read(c):null;}}
    public Chemical byUid(long uid){try(Cursor c=db.getReadableDatabase().rawQuery("SELECT "+COLS+" FROM v14_unified_chemical_catalog WHERE uid=?",new String[]{String.valueOf(uid)})){return c.moveToFirst()?read(c):null;}}
    /**
     * Complete source-scoped V13 material profile. Shared by tracking,
     * candidate details, developer lab and export. Null means UNKNOWN,
     * never "no hazard"; any ChemCore metric is evidence, not G3 validation.
     */
    public JSONObject scientificProfile(long uid){
        JSONObject out=new JSONObject();
        if(uid<=0)return out;
        try(Cursor c=db.getReadableDatabase().rawQuery(
                "SELECT * FROM v14_unified_chemical_catalog WHERE uid=? LIMIT 1",
                new String[]{Long.toString(uid)})){
            if(!c.moveToFirst())return out;
            for(int i=0;i<c.getColumnCount();i++){
                Object value;
                switch(c.getType(i)){
                    case Cursor.FIELD_TYPE_INTEGER:value=c.getLong(i);break;
                    case Cursor.FIELD_TYPE_FLOAT:value=c.getDouble(i);break;
                    case Cursor.FIELD_TYPE_STRING:value=c.getString(i);break;
                    default:value=JSONObject.NULL;
                }
                out.put(c.getColumnName(i),value);
            }
        }catch(Exception ex){
            android.util.Log.w("TankUnifiedChemCatalog",
                "Cannot read source-qualified hazard profile uid="+uid,ex);
        }
        // A synonym pair from the NIOSH Pocket Guide points to the same
        // substance but does NOT erase the legacy UID or transplant exposure
        // evidence into old approved experiments. Surface both directions of
        // the crosswalk so consumers can resolve the master identity deliberately.
        JSONArray identityAliases=new JSONArray();
        try(Cursor alias=db.getReadableDatabase().rawQuery(
                "SELECT x.alias_uid,x.canonical_uid,x.source_cas,x.source_id,"+
                "x.source_url,x.evidence_status,a.name_en,b.name_en "+
                "FROM chemical_identity_equivalence_v6 x "+
                "JOIN chemicals a ON a.uid=x.alias_uid "+
                "JOIN chemicals b ON b.uid=x.canonical_uid "+
                "WHERE x.alias_uid=? OR x.canonical_uid=? "+
                "ORDER BY x.alias_uid LIMIT 100",
                new String[]{Long.toString(uid),Long.toString(uid)})){
            while(alias.moveToNext()){
                JSONObject item=new JSONObject();
                item.put("alias_uid",alias.getLong(0));
                item.put("canonical_uid",alias.getLong(1));
                item.put("cas",alias.getString(2));
                item.put("source_id",alias.getString(3));
                item.put("source_url",alias.getString(4));
                item.put("evidence_status",alias.getString(5));
                item.put("historical_name",alias.getString(6));
                item.put("canonical_name",alias.getString(7));
                item.put("relation_to_query",alias.getLong(0)==uid?
                    "HISTORICAL_ALIAS_OF_CANONICAL_UID":"CANONICAL_FOR_HISTORICAL_ALIAS");
                item.put("automatic_measurement_rekey_performed",false);
                identityAliases.put(item);
            }
        }catch(Exception e){
            android.util.Log.w("TankUnifiedChemCatalog",
                "NIOSH synonym crosswalk unavailable for uid="+uid,e);
        }
        try{
            out.put("source_qualified_identity_aliases",identityAliases);
            out.put("source_qualified_identity_alias_count",identityAliases.length());
            out.put("alias_link_is_not_new_gas_detection_evidence",true);
        }catch(Exception ignored){}
        // These observations are linked via applicable MQ sensor MODELS,
        // never via chemical identity. An external sensor's R0/Rs MUST NOT
        // populate this material's operational R0/ppm or G3 fields.
        JSONArray peers=new JSONArray();
        try(Cursor study=db.getReadableDatabase().rawQuery(
                "SELECT se.coefficients,se.source_id,se.sensor_key,se.status,ss.url "+
                "FROM sensor_evidence se JOIN scientific_sources ss ON "+
                "ss.source_id=se.source_id WHERE se.chemical_uid IS NULL "+
                "AND se.evidence_type='EXTERNAL_PEER_R0_RS' "+
                "AND EXISTS(SELECT 1 FROM mq_chemical_reference_curves mc "+
                "WHERE mc.chemical_uid=? AND mc.sensor_model=se.sensor_key) "+
                "ORDER BY se.sensor_key,se.source_id,se.id LIMIT 100",
                new String[]{Long.toString(uid)})){
            while(study.moveToNext()){
                JSONObject evidence=new JSONObject(study.getString(0));
                evidence.put("source_id",study.getString(1));
                evidence.put("sensor_model",study.getString(2));
                evidence.put("evidence_status",study.getString(3));
                evidence.put("source_url",study.isNull(4)?JSONObject.NULL:study.getString(4));
                evidence.put("scope","OTHER_DEVICE_SENSOR_RESEARCH_ONLY");
                evidence.put("is_material_specific_r0",false);
                evidence.put("usable_as_tank_r0",false);
                peers.put(evidence);
            }
        }catch(Exception err){
            android.util.Log.w("TankUnifiedChemCatalog",
                "Peer sensor R0/Rs research references not readable",err);
        }
        try{
            out.put("external_mq_r0_rs_sensor_studies",peers);
            out.put("external_mq_r0_rs_study_count",peers.length());
            out.put("external_r0_rs_not_material_constants",true);
        }catch(Exception ignored){}
        // Canonical per-material curve evidence is *joined* by UID. R0 from a
        // source curve can never replace a particular Tank MQ device baseline.
        JSONArray curves=new JSONArray();
        try(Cursor c=db.getReadableDatabase().rawQuery(
                "SELECT m.curve_id,m.sensor_model,m.ratio_a,m.ratio_b,m.output_scale,"+
                "m.min_source_ppm,m.max_source_ppm,m.reference_r0_definition,"+
                "m.reference_r0_kohm,m.reference_rs_kohm,m.source_id,"+
                "s.url,m.evidence_status,m.heater_profile "+
                "FROM mq_chemical_reference_curves m JOIN scientific_sources s "+
                "ON s.source_id=m.source_id WHERE m.chemical_uid=? "+
                "ORDER BY m.sensor_model,m.curve_id LIMIT 120",
                new String[]{Long.toString(uid)})){
            while(c.moveToNext()){
                JSONObject item=new JSONObject();
                item.put("curve_id",c.getString(0));
                item.put("sensor_model",c.getString(1));
                item.put("ratio_a",c.getDouble(2));
                item.put("ratio_b",c.getDouble(3));
                item.put("output_scale",c.getDouble(4));
                item.put("source_min_ppm",c.isNull(5)?JSONObject.NULL:c.getDouble(5));
                item.put("source_max_ppm",c.isNull(6)?JSONObject.NULL:c.getDouble(6));
                item.put("r0_definition",c.getString(7));
                item.put("source_r0_kohm",c.isNull(8)?JSONObject.NULL:c.getDouble(8));
                item.put("source_rs_kohm",c.isNull(9)?JSONObject.NULL:c.getDouble(9));
                item.put("source_id",c.getString(10));
                item.put("source_url",c.isNull(11)?JSONObject.NULL:c.getString(11));
                item.put("evidence_status",c.getString(12));
                item.put("heater_profile",c.isNull(13)?JSONObject.NULL:c.getString(13));
                item.put("can_calibrate_tank_r0",false);
                item.put("can_certify_gas_ppm",false);
                curves.put(item);
            }
        }catch(Exception e){
            android.util.Log.w("TankUnifiedChemCatalog","Cannot read referenced MQ curves",e);
        }
        JSONArray exposure=new JSONArray();
        try(Cursor c=db.getReadableDatabase().rawQuery(
                "SELECT e.authority,e.limit_type,e.value,e.unit,e.duration_min,"+
                "e.source_id,e.jurisdiction,e.notation,s.url "+
                "FROM exposure_limits e LEFT JOIN scientific_sources s "+
                "ON s.source_id=e.source_id WHERE e.chemical_uid=? "+
                "ORDER BY e.authority,e.limit_type,e.id LIMIT 200",
                new String[]{Long.toString(uid)})){
            while(c.moveToNext()){
                JSONObject item=new JSONObject();
                item.put("authority",c.getString(0));
                item.put("limit_type",c.getString(1));
                item.put("value",c.isNull(2)?JSONObject.NULL:c.getDouble(2));
                item.put("unit",c.isNull(3)?JSONObject.NULL:c.getString(3));
                item.put("duration_minutes",c.isNull(4)?JSONObject.NULL:c.getDouble(4));
                item.put("source_id",c.isNull(5)?JSONObject.NULL:c.getString(5));
                item.put("jurisdiction",c.isNull(6)?JSONObject.NULL:c.getString(6));
                item.put("notation",c.isNull(7)?JSONObject.NULL:c.getString(7));
                item.put("source_url",c.isNull(8)?JSONObject.NULL:c.getString(8));
                item.put("is_background_normality_band",false);
                exposure.put(item);
            }
        }catch(Exception e){
            android.util.Log.w("TankUnifiedChemCatalog","Cannot read source exposure limits",e);
        }
        JSONArray hazards=new JSONArray();
        try(Cursor c=db.getReadableDatabase().rawQuery(
                "SELECT h.hazard_category,h.classification_system,h.classification_detail,"+
                "h.ghs_pictogram,h.ghs_h_codes,h.source_id,h.evidence_status,s.url "+
                "FROM chemical_hazard_classifications h JOIN scientific_sources s "+
                "ON s.source_id=h.source_id WHERE h.chemical_uid=? "+
                "ORDER BY h.hazard_category,h.source_id,h.id LIMIT 120",
                new String[]{Long.toString(uid)})){
            while(c.moveToNext()){
                JSONObject item=new JSONObject();
                item.put("hazard_category",c.getString(0));
                item.put("classification_system",c.getString(1));
                item.put("classification_detail",c.getString(2));
                item.put("ghs_pictogram",c.isNull(3)?JSONObject.NULL:c.getString(3));
                item.put("ghs_h_codes",c.isNull(4)?JSONObject.NULL:c.getString(4));
                item.put("source_id",c.getString(5));
                item.put("evidence_status",c.getString(6));
                item.put("source_url",c.isNull(7)?JSONObject.NULL:c.getString(7));
                item.put("gas_detected",false);
                item.put("hazard_threshold_exceeded",false);
                hazards.put(item);
            }
        }catch(Exception e){
            android.util.Log.w("TankUnifiedChemCatalog","Cannot read source hazard classifications",e);
        }
        // A formula or molar mass imported from the existing offline PubChem
        // seed must remain traceable to its actual property evidence. It is
        // not a Tank detection model or R0/Rs for this material.
        JSONArray molecularIdentity=new JSONArray();
        try(Cursor p=db.getReadableDatabase().rawQuery(
                "SELECT property_key,value_numeric,value_text,unit,method,source_id,"+
                "quality_tier FROM chemcore_properties WHERE chemical_uid=? "+
                "AND source_id='PUBCHEM' AND quality_tier='A_SOURCE' "+
                "AND property_key IN ('MolecularFormula','MolecularWeight') "+
                "ORDER BY property_key,id DESC LIMIT 20",
                new String[]{Long.toString(uid)})){
            while(p.moveToNext()){
                JSONObject fact=new JSONObject();
                fact.put("property_key",p.getString(0));
                fact.put("numeric_value",p.isNull(1)?JSONObject.NULL:p.getDouble(1));
                fact.put("text_value",p.isNull(2)?JSONObject.NULL:p.getString(2));
                fact.put("unit",p.isNull(3)?JSONObject.NULL:p.getString(3));
                fact.put("method",p.isNull(4)?JSONObject.NULL:p.getString(4));
                fact.put("source_id",p.getString(5));
                fact.put("quality_tier",p.getString(6));
                fact.put("is_gas_detection_evidence",false);
                molecularIdentity.put(fact);
            }
        }catch(Exception ex){
            android.util.Log.w("TankUnifiedChemCatalog",
                "PubChem identity evidence not loaded for uid="+uid,ex);
        }
        try{
            out.put("molecular_identity_source_facts",molecularIdentity);
            out.put("molecular_identity_source_fact_count",molecularIdentity.length());
            out.put("mq_reference_curve_evidence",curves);
            out.put("mq_reference_curve_evidence_count",curves.length());
            out.put("occupational_exposure_source_limits",exposure);
            out.put("occupational_exposure_source_limit_count",exposure.length());
            out.put("source_qualified_hazard_classifications",hazards);
            out.put("source_qualified_hazard_classification_count",hazards.length());
            out.put("qualified_tank_g3",false);
            out.put("derived_hazard_notifications",false);
            out.put("source_profile_schema","hazrob-tank-unified-chemical-v14-plus-evidence/1");
        }catch(Exception ignored){}
        return out;
    }

    public List<String> identifiers(long uid,String scheme){List<String> out=new ArrayList<>();try(Cursor c=db.getReadableDatabase().rawQuery("SELECT DISTINCT value FROM chemical_identifiers WHERE chemical_uid=? AND scheme=? ORDER BY value",new String[]{String.valueOf(uid),scheme})){while(c.moveToNext())out.add(c.getString(0));}return out;}

    private Chemical exact(String q){
        String sql="SELECT DISTINCT c.uid,c.legacy_id,c.cas,c.pubchem_cid,c.name_en,c.name_ar,c.formula,c.molar_mass,c.chemical_class,c.record_source FROM v14_unified_chemical_catalog c LEFT JOIN chemical_synonyms s ON s.chemical_uid=c.uid LEFT JOIN chemical_identifiers i ON i.chemical_uid=c.uid WHERE (c.cas=? OR i.value=? OR lower(trim(c.name_en))=lower(trim(?)) OR lower(trim(c.name_ar))=lower(trim(?)) OR lower(trim(s.synonym))=lower(trim(?))) ORDER BY c.uid";
        try(Cursor x=db.getReadableDatabase().rawQuery(sql,new String[]{q,q,q,q,q})){Chemical one=null;int n=0;while(x.moveToNext()){one=read(x);n++;}return n==1?one:null;}
    }

    private static Chemical read(Cursor c){
        Integer legacy=c.isNull(1)?null:c.getInt(1);Integer pubchem=c.isNull(3)?null:c.getInt(3);Double mw=c.isNull(7)?null:c.getDouble(7);
        return new Chemical(c.getLong(0),legacy,c.getString(2),pubchem,c.getString(4),c.getString(5),c.getString(6),mw,c.getString(8),c.getString(9));
    }
}
