package org.candlepin.model;

public class PoolProductAttribute {
    private String poolId;
    private String name;
    private String value;
    
    public PoolProductAttribute(String poolId, String name, String value) {
        super();
        this.poolId = poolId;
        this.name = name;
        this.value= value;
    }

    public String getPoolId() {
        return poolId;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    
    
}
