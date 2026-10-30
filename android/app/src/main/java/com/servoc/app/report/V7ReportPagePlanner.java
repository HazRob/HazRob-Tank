package com.servoc.app.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Report-only pagination planner. Does not interpret sensor readings or
 * suppress report sections based on measurement availability.
 *
 * Page count is intentionally dynamic. Section headers are repeated on
 * continuation pages and every source row is preserved. The caller is
 * responsible for measuring Arabic shaped text and selecting row heights
 * sufficient for a minimum 12pt font before invoking this planner.
 *
 * Each Item is an indivisible print unit (one wrapped row or one chart).
 * Oversized source items fail explicitly instead of being clipped.
 */
public final class V7ReportPagePlanner {
    private V7ReportPagePlanner() {}

    public static final class Item {
        public final String id;
        public final float heightPt;
        public Item(String id,float heightPt){
            if(id==null||id.trim().isEmpty())throw new IllegalArgumentException("blank item ID");
            if(!Float.isFinite(heightPt)||heightPt<=0)
                throw new IllegalArgumentException("nonpositive item height: "+id);
            this.id=id;
            this.heightPt=heightPt;
        }
    }
    public static final class Section {
        public final String id;
        public final float titleHeightPt;
        public final List<Item> items;
        public Section(String id,float titleHeightPt,List<Item> items){
            if(id==null||id.trim().isEmpty())throw new IllegalArgumentException("blank section ID");
            if(!Float.isFinite(titleHeightPt)||titleHeightPt<=0)
                throw new IllegalArgumentException("nonpositive section header");
            if(items==null)throw new IllegalArgumentException("missing section items");
            this.id=id;
            this.titleHeightPt=titleHeightPt;
            this.items=Collections.unmodifiableList(new ArrayList<>(items));
        }
    }
    public static final class Element {
        public final String sectionId;
        public final String itemId;
        public final float topPt,heightPt;
        public final boolean sectionHeader;
        private Element(String section,String item,float top,float height,boolean header){
            sectionId=section;itemId=item;topPt=top;heightPt=height;sectionHeader=header;
        }
    }
    public static final class Page {
        public final int number;
        public final List<Element> elements;
        private Page(int n,List<Element> content){
            number=n;elements=Collections.unmodifiableList(new ArrayList<>(content));
        }
    }

    /**
     * @param contentTopPt top of the printable report body after its header
     * @param contentBottomPt bottom of the body before its footer
     * @param sections all sections in official owner-approved order
     * @return immutable pages numbered 1..N, with no arbitrary maximum N.
     */
    public static List<Page> plan(float contentTopPt,float contentBottomPt,
                                  List<Section> sections){
        if(!Float.isFinite(contentTopPt)||!Float.isFinite(contentBottomPt)||
            contentBottomPt<=contentTopPt)
            throw new IllegalArgumentException("invalid printable body");
        if(sections==null)throw new IllegalArgumentException("null sections");

        final float capacity=contentBottomPt-contentTopPt;
        List<Page> result=new ArrayList<>();
        List<Element> current=new ArrayList<>();
        float used=0f;

        for(Section section:sections){
            if(section==null)throw new IllegalArgumentException("null section");
            if(section.titleHeightPt>capacity)
                throw new IllegalArgumentException("section title exceeds page: "+section.id);
            if(section.items.isEmpty()){
                if(used+section.titleHeightPt>capacity && !current.isEmpty()){
                    result.add(new Page(result.size()+1,current));
                    current=new ArrayList<>();used=0;
                }
                current.add(new Element(section.id,null,contentTopPt+used,
                    section.titleHeightPt,true));
                used+=section.titleHeightPt;
                continue;
            }

            // Avoid orphan headings: reserve enough space for first row.
            Item first=section.items.get(0);
            if(section.titleHeightPt+first.heightPt>capacity)
                throw new IllegalArgumentException("row too tall for repeated heading: "+first.id);
            if(used+section.titleHeightPt+first.heightPt>capacity && !current.isEmpty()){
                result.add(new Page(result.size()+1,current));
                current=new ArrayList<>();used=0;
            }
            current.add(new Element(section.id,null,contentTopPt+used,
                section.titleHeightPt,true));
            used+=section.titleHeightPt;
            for(Item item:section.items){
                if(item==null)throw new IllegalArgumentException("null item");
                if(item.heightPt+section.titleHeightPt>capacity)
                    throw new IllegalArgumentException("item taller than page: "+item.id);
                if(used+item.heightPt>capacity){
                    result.add(new Page(result.size()+1,current));
                    current=new ArrayList<>();used=0;
                    // repeat title with the same semantics on continuation page
                    current.add(new Element(section.id,null,contentTopPt+used,
                        section.titleHeightPt,true));
                    used+=section.titleHeightPt;
                }
                current.add(new Element(section.id,item.id,
                    contentTopPt+used,item.heightPt,false));
                used+=item.heightPt;
            }
        }
        if(!current.isEmpty()||result.isEmpty())
            result.add(new Page(result.size()+1,current));
        return Collections.unmodifiableList(result);
    }
}
