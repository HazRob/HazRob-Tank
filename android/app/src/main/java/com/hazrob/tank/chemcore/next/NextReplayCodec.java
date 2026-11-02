package com.hazrob.tank.chemcore.next;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * P01.4: versioned, byte-faithful replay for immutable snapshots and research decisions.
 *
 * Pure JDK/Android API: no UI, network, SQLite, or JSON library dependencies.
 * This is an internal scientific replay envelope, NOT a cryptographic signature
 * or a public cross-platform exchange format. A JSON adapter will be versioned
 * separately during P07 integration.
 *
 * Envelope: HCNEXT-REPLAY/1:<sha256hex>:<base64url-no-padding(binary)>
 * Binary: magic, record kind, schema version, then length-prefixed UTF-8 fields.
 *
 * Integrity detects accidental mutation, not authenticity (no secret/signature).
 */
public final class NextReplayCodec {
    public static final String ENVELOPE = "HCNEXT-REPLAY/1:";
    public static final int VERSION = 1;
    private static final int MAGIC = 0x48434E58; // HCNX
    private static final int SNAPSHOT = 1, RESULT = 2;
    private static final int MAX_INPUT_BYTES = 2_000_000;
    private static final int MAX_STRING_BYTES = 16_384;
    private static final int MAX_SENSORS = 128, MAX_NODES = 4096, MAX_CANDIDATES = 2048,
                             MAX_ROOTS = 1024, MAX_PROBLEMS = 4096;

    private NextReplayCodec() {}

    public static String encodeSnapshot(CoreContracts.Snapshot snapshot) {
        if (snapshot == null) throw new IllegalArgumentException("snapshot required");
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            header(out, SNAPSHOT);
            putString(out, CoreContracts.SCHEMA);
            putString(out, snapshot.uid);
            putString(out, snapshot.deviceUid);
            putString(out, snapshot.mode.name());
            out.writeLong(snapshot.monotonicMillis);
            putDouble(out, snapshot.temperatureC);
            putDouble(out, snapshot.relativeHumidityPercent);
            out.writeBoolean(snapshot.mqRelayOn);
            putString(out, snapshot.stableManifestHash);
            count(out, snapshot.readings.size(), MAX_SENSORS, "sensor count");
            for (CoreContracts.Reading r : snapshot.readings) {
                putString(out, r.sensorUid);
                out.writeBoolean(r.rawAdc != null);
                if (r.rawAdc != null) out.writeInt(r.rawAdc);
                putDouble(out, r.voltageV);
                putDouble(out, r.rsKohm);
                putDouble(out, r.rAirKohm);
                putDouble(out, r.r0SourceKohm);
                putString(out, r.r0Definition.name());
                putString(out, r.heaterPhase);
                putString(out, r.quality.name());
            }
            out.flush();
            return envelope(bytes.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("snapshot encoding failed", e);
        }
    }

    public static CoreContracts.Snapshot decodeSnapshot(String encoded) {
        DataInputStream in = open(encoded, SNAPSHOT);
        try {
            String schema = getString(in);
            if (!CoreContracts.SCHEMA.equals(schema))
                throw new IllegalArgumentException("unsupported snapshot schema: " + schema);
            String uid = getString(in), deviceUid = getString(in);
            CoreContracts.Mode mode = enumName(CoreContracts.Mode.class, getString(in));
            long time = in.readLong();
            Double temperature = getDouble(in), rh = getDouble(in);
            boolean relay = in.readBoolean();
            String stable = getString(in);
            int n = takeCount(in, MAX_SENSORS, "sensor count");
            List<CoreContracts.Reading> readings = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                String sensor = getString(in);
                Integer raw = in.readBoolean() ? in.readInt() : null;
                Double voltage = getDouble(in), rs = getDouble(in),
                        air = getDouble(in), r0 = getDouble(in);
                CoreContracts.CalibrationReference definition =
                        enumName(CoreContracts.CalibrationReference.class, getString(in));
                String phase = getString(in);
                CoreContracts.SampleQuality quality =
                        enumName(CoreContracts.SampleQuality.class, getString(in));
                readings.add(new CoreContracts.Reading(sensor,raw,voltage,rs,air,r0,
                                                        definition,phase,quality));
            }
            mustFinish(in);
            CoreContracts.Snapshot snapshot = new CoreContracts.Snapshot(uid,deviceUid,mode,
                time,temperature,rh,relay,readings,stable);
            if (!encoded.equals(encodeSnapshot(snapshot)))
                throw new IllegalArgumentException("noncanonical snapshot or replay changed a value");
            return snapshot;
        } catch (IOException e) {
            throw new IllegalArgumentException("truncated snapshot replay", e);
        }
    }

    public static String encodeResult(CoreContracts.Result result) {
        if (result == null) throw new IllegalArgumentException("result required");
        if (result.reportablePpm || result.physicallyValidatedG3)
            throw new IllegalArgumentException("research result must never claim calibrated ppm/G3");
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            header(out, RESULT);
            putString(out, result.schema);
            putString(out, result.engineVersion);
            putString(out, result.snapshotUid);
            putString(out, result.snapshotHash);
            putString(out, result.stableManifestHash);
            putString(out, result.status.name());
            count(out, result.evidence.size(), MAX_NODES, "evidence count");
            for (CoreContracts.EvidenceNode node : result.evidence) {
                putString(out, node.sourceRootId);
                putString(out, node.expertId);
                putString(out, node.sensorUid);
                out.writeBoolean(node.canonicalChemicalUid != null);
                if (node.canonicalChemicalUid != null) out.writeLong(node.canonicalChemicalUid);
                putString(out, node.polarity.name());
                putString(out, node.note);
            }
            count(out, result.rankedEligibleCandidates.size(), MAX_CANDIDATES,"candidate count");
            for (CoreContracts.Candidate candidate : result.rankedEligibleCandidates) {
                out.writeLong(candidate.canonicalChemicalUid);
                putString(out, candidate.evidenceSummary);
                count(out, candidate.sourceRoots.size(), MAX_ROOTS, "source root count");
                for (String root : candidate.sourceRoots) putString(out, root);
            }
            count(out, result.problems.size(), MAX_PROBLEMS, "problem count");
            for (String issue : result.problems) putString(out, issue);
            out.flush();
            return envelope(bytes.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("result encoding failed", e);
        }
    }

    /** A decision must be decoded against the very snapshot it refers to. */
    public static CoreContracts.Result decodeResult(String encoded, CoreContracts.Snapshot snapshot) {
        if (snapshot == null) throw new IllegalArgumentException("matching snapshot required");
        DataInputStream in = open(encoded, RESULT);
        try {
            String schema = getString(in);
            if (!CoreContracts.SCHEMA.equals(schema))
                throw new IllegalArgumentException("unsupported result schema");
            String engine = getString(in), uid = getString(in), hash = getString(in),
                    stable = getString(in);
            if (!snapshot.uid.equals(uid) || !ChemCoreNext.snapshotHash(snapshot).equals(hash)
                    || !snapshot.stableManifestHash.equals(stable))
                throw new IllegalArgumentException("result does not match supplied immutable snapshot");
            CoreContracts.DecisionStatus status =
                    enumName(CoreContracts.DecisionStatus.class, getString(in));
            int n = takeCount(in, MAX_NODES, "evidence count");
            List<CoreContracts.EvidenceNode> evidence = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                String root = getString(in), expert = getString(in), sensor = getString(in);
                Long chemical = in.readBoolean() ? in.readLong() : null;
                CoreContracts.EvidencePolarity polarity =
                    enumName(CoreContracts.EvidencePolarity.class, getString(in));
                evidence.add(new CoreContracts.EvidenceNode(root,expert,sensor,chemical,
                                                             polarity,getString(in)));
            }
            int c = takeCount(in,MAX_CANDIDATES,"candidate count");
            List<CoreContracts.Candidate> candidates = new ArrayList<>(c);
            for(int i=0;i<c;i++) {
                long chemical=in.readLong();
                String summary=getString(in);
                int k=takeCount(in,MAX_ROOTS,"source root count");
                List<String> roots=new ArrayList<>(k);
                for(int j=0;j<k;j++) roots.add(getString(in));
                candidates.add(new CoreContracts.Candidate(chemical,summary,roots));
            }
            int p=takeCount(in,MAX_PROBLEMS,"problem count");
            List<String> problems=new ArrayList<>(p);
            for(int i=0;i<p;i++) problems.add(getString(in));
            mustFinish(in);
            CoreContracts.Result reconstructed=new CoreContracts.Result(engine,snapshot,hash,
                status,evidence,candidates,problems);
            if (!encoded.equals(encodeResult(reconstructed)))
                throw new IllegalArgumentException("noncanonical research decision replay");
            return reconstructed;
        } catch(IOException e) {
            throw new IllegalArgumentException("truncated decision replay",e);
        }
    }

    private static void header(DataOutputStream out,int kind)throws IOException{
        out.writeInt(MAGIC);out.writeInt(VERSION);out.writeInt(kind);
    }
    private static DataInputStream open(String encoded,int kind){
        if(encoded==null||!encoded.startsWith(ENVELOPE))
            throw new IllegalArgumentException("unsupported replay envelope version");
        int start=ENVELOPE.length();
        int colon=encoded.indexOf(':',start);
        if(colon<0) throw new IllegalArgumentException("replay hash missing");
        String expected=encoded.substring(start,colon);
        if(!expected.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("invalid replay digest");
        String payload=encoded.substring(colon+1);
        if(payload.length()>((MAX_INPUT_BYTES*4L+2)/3)+8)
            throw new IllegalArgumentException("replay input too large");
        byte[] decoded;
        try{decoded=Base64.getUrlDecoder().decode(payload);}
        catch(IllegalArgumentException e){throw new IllegalArgumentException("invalid replay base64",e);}
        if(decoded.length>MAX_INPUT_BYTES||decoded.length<12)
            throw new IllegalArgumentException("replay byte size out of bounds");
        if(!digest(decoded).equals(expected))
            throw new IllegalArgumentException("replay SHA-256 mismatch");
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(decoded));
        try {
            if(in.readInt()!=MAGIC) throw new IllegalArgumentException("bad replay magic");
            if(in.readInt()!=VERSION) throw new IllegalArgumentException("unsupported replay binary version");
            if(in.readInt()!=kind) throw new IllegalArgumentException("wrong replay record kind");
        }catch(IOException e){throw new IllegalArgumentException("truncated replay header",e);}
        return in;
    }
    private static String envelope(byte[] payload){
        if(payload.length>MAX_INPUT_BYTES) throw new IllegalArgumentException("replay payload too large");
        return ENVELOPE+digest(payload)+":"+Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
    }
    private static String digest(byte[] bytes){
        try{
            byte[] hash=MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder b=new StringBuilder(64);
            for(byte v:hash) b.append(String.format(Locale.ROOT,"%02x",v&255));
            return b.toString();
        }catch(NoSuchAlgorithmException e){throw new IllegalStateException("SHA-256 unavailable",e);}
    }
    private static void putString(DataOutputStream out,String s)throws IOException{
        if(s==null) throw new IllegalArgumentException("null string in replay");
        byte[] bytes=s.getBytes(StandardCharsets.UTF_8);
        if(bytes.length>MAX_STRING_BYTES)throw new IllegalArgumentException("replay string too long");
        out.writeInt(bytes.length);out.write(bytes);
    }
    private static String getString(DataInputStream in)throws IOException{
        int len=in.readInt();
        if(len<0||len>MAX_STRING_BYTES)throw new IllegalArgumentException("bad replay text length");
        byte[] b=new byte[len];in.readFully(b);
        String s=new String(b,StandardCharsets.UTF_8);
        if(!java.util.Arrays.equals(s.getBytes(StandardCharsets.UTF_8),b))
            throw new IllegalArgumentException("invalid UTF-8 replay text");
        return s;
    }
    private static void putDouble(DataOutputStream out,Double n)throws IOException {
        out.writeBoolean(n!=null);
        if(n!=null)out.writeLong(Double.doubleToRawLongBits(n));
    }
    private static Double getDouble(DataInputStream in)throws IOException {
        return in.readBoolean()?Double.longBitsToDouble(in.readLong()):null;
    }
    private static void count(DataOutputStream out,int n,int max,String label)throws IOException{
        if(n<0||n>max)throw new IllegalArgumentException(label+" exceeds research limit");
        out.writeInt(n);
    }
    private static int takeCount(DataInputStream in,int max,String label)throws IOException{
        int value=in.readInt();
        if(value<0||value>max)throw new IllegalArgumentException("bad "+label);
        return value;
    }
    private static <T extends Enum<T>> T enumName(Class<T> klass,String name){
        try{return Enum.valueOf(klass,name);}
        catch(IllegalArgumentException e){throw new IllegalArgumentException("unknown replay enum: "+name,e);}
    }
    private static void mustFinish(DataInputStream in)throws IOException{
        if(in.read()!=-1)throw new IllegalArgumentException("unexpected trailing replay bytes");
    }
}
