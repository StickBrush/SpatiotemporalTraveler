package nathanielwendt.mpc.ut.edu.paco;

import com.ut.mpc.utils.STRegion;

/**
 * Created by nathanielwendt on 11/25/16.
 */
public class PlaceData {
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public STRegion getRegion() {
        return region;
    }

    public void setRegion(STRegion region) {
        this.region = region;
    }

    public String getCoverage() {
        return coverage;
    }

    public void setCoverage(String coverage) {
        this.coverage = coverage;
    }

    private String name;
    private String uri;
    private STRegion region;
    private String coverage;

    public PlaceData(String name, String uri, STRegion region){
        this.name = name;
        this.uri = uri;
        this.region = region;
    }

}

