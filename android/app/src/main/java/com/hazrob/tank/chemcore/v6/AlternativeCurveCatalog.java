package com.hazrob.tank.chemcore.v6;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Six alternative digitized MQ135 curves, another RESEARCH source family.
 * Source immutable Git blob 393f304c54027bb6f19e7bcd47309c4506f7f2b7.
 * Literature regression fit is not a physical calibration on Tank.
 */
public final class AlternativeCurveCatalog {
    public static final String SOURCE_GIT_BLOB="393f304c54027bb6f19e7bcd47309c4506f7f2b7";
    public static final String SOURCE_DOI="10.23960/jtaf.v11i2.314";
    private AlternativeCurveCatalog(){}
    private static final List<SourceCatalog.Curve> ENTRIES;
    static{
        List<SourceCatalog.Curve> tmp=new ArrayList<>();
        Collections.addAll(tmp,
        new SourceCatalog.Curve(44, "MQ135", 110L, "Acetone", 2.7295, -0.2822, 10, 100, 1.0),
        new SourceCatalog.Curve(45, "MQ135", 95L, "Toluene", 2.963, -0.2838, 10, 100, 1.0),
        new SourceCatalog.Curve(46, "MQ135", 103L, "Ethanol/alcohol", 3.8659, -0.3091, 10, 100, 1.0),
        new SourceCatalog.Curve(47, "MQ135", 23L, "Carbon Dioxide", 4.9999, -0.3387, 10, 100, 1.0),
        new SourceCatalog.Curve(48, "MQ135", 21L, "Ammonia", 6.4835, -0.406, 10, 100, 1.0),
        new SourceCatalog.Curve(49, "MQ135", 20L, "Carbon Monoxide", 5.2629, -0.2668, 10, 100, 1.0)
        );
        ENTRIES=Collections.unmodifiableList(tmp);
    }
    public static List<SourceCatalog.Curve> curves(){return ENTRIES;}
}
