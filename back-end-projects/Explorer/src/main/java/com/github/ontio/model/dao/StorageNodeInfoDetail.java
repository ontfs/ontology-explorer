package com.github.ontio.model.dao;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Table;
import java.math.BigInteger;

@Data
@EqualsAndHashCode(callSuper = true)
public class StorageNodeInfoDetail extends StorageNodeInfo {

    private BigInteger pledge;
    private BigInteger profit;
    private String volume;
    private String restVol;
    private BigInteger serviceTime;
    private BigInteger minPdpInterval;


}
