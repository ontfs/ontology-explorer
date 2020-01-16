/*
 * Copyright (C) 2018 The ontology Authors
 * This file is part of The ontology library.
 *
 * The ontology is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ontology is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with The ontology.  If not, see <http://www.gnu.org/licenses/>.
 */


package com.github.ontio.util;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.ontio.OntSdk;
import com.github.ontio.common.Address;
import com.github.ontio.common.Helper;
import com.github.ontio.config.ParamsConfig;
import com.github.ontio.core.payload.InvokeWasmCode;
import com.github.ontio.core.transaction.Transaction;
import com.github.ontio.io.BinaryReader;
import com.github.ontio.model.dao.StorageNodeInfoDetail;
import com.github.ontio.sdk.exception.SDKException;
import com.github.ontio.smartcontract.nativevm.abi.NativeBuildParams;
import com.github.ontio.smartcontract.nativevm.abi.Struct;
import com.github.ontio.smartcontract.neovm.abi.BuildParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * @author zhouq
 * @date 2018/2/27
 */
@Slf4j
@Component
public class OntologySDKService {

    private static OntologySDKService instance = null;

    @Autowired
    private ParamsConfig paramsConfig;

    public static synchronized OntologySDKService getInstance(ParamsConfig param) {
        if (instance == null) {
            instance = new OntologySDKService(param);
        }
        return instance;
    }

    public OntologySDKService(ParamsConfig param) {
        this.paramsConfig = param;
    }

    public String getDDO(String ontId) {
        OntSdk ontSdk = getOntSdk();
        String ddoStr = "";
        try {
            ddoStr = ontSdk.nativevm().ontId().sendGetDDO(ontId);
        } catch (Exception e) {
            log.error("getDDO error...", e);
            e.printStackTrace();
        }

        return ddoStr;
    }


    /**
     * 获取账户余额，包括unboundong
     *
     * @param address
     * @return
     */
    public Map getNativeAssetBalance(String address) {
        Map<String, Object> balanceMap = new HashMap<>();
        try {
            OntSdk ontSdk = getOntSdk();
            balanceMap = (Map) ontSdk.getConnect().getBalance(address);
        } catch (Exception e) {
            log.error("getNativeAssetBalance error...", e);
            balanceMap.put("ong", "0");
            balanceMap.put("ont", "0");
        }
        return balanceMap;
    }

    /**
     * get neovm OEP4 balance
     *
     * @param address
     * @return
     */
    public String getNeovmOep4AssetBalance(String address, String contractHash) {
        OntSdk ontSdk = getOep4OntSdk(contractHash);
        try {
            String balance = ontSdk.neovm().oep4().queryBalanceOf(address);
            return balance;
        } catch (Exception e) {
            log.error("getNeovmOep4AssetBalance error...", e);
            return "0";
        }
    }


    /**
     * get neovm OEP4 balance
     *
     * @param address
     * @return
     */
    public String getWasmvmOep4AssetBalance(String address, String contractHash) {
        try {
            OntSdk ontSdk = getOntSdk();

            List<Object> params = new ArrayList<>(Collections.singletonList(Address.decodeBase58(address)));
            InvokeWasmCode tx = ontSdk.wasmvm().makeInvokeCodeTransaction(contractHash, "balanceOf", params, Address.decodeBase58(address), 500, 25000000);
            JSONObject result = (JSONObject) ontSdk.getRestful().sendRawTransactionPreExec(tx.toHexString());
            log.info("getWasmvmOep4AssetBalance result:{}", result);
            BigDecimal decimal = new BigDecimal(Helper.BigIntFromNeoBytes(Helper.hexToBytes(result.getString("Result"))).longValue());
            return decimal.stripTrailingZeros().toPlainString();
        } catch (Exception e) {
            log.error("getNeovmOep4AssetBalance error...", e);
            return "0";
        }
    }


    /**
     * 获取OEP5账户余额
     *
     * @param address
     * @return
     */
    public String getOep5AssetBalance(String address, String contractAddr) {
        OntSdk ontSdk = getOep5OntSdk(contractAddr);
        try {
            String balance = ontSdk.neovm().oep5().queryBalanceOf(address);
            return balance;
        } catch (Exception e) {
            log.error("getOep5AssetBalance error...", e);
            return "0";
        }
    }

    /**
     * 获取账户南瓜余额，包括unboundong
     *
     * @param address
     * @return
     */
    public JSONArray getOpe8AssetBalance(String address, String codeHash) {
        JSONArray balanceArray = new JSONArray();
        try {
            OntSdk ontSdk = getOep8OntSdk(codeHash);
            String balance = ontSdk.neovm().oep8().balancesOf(address);
            balanceArray = JSON.parseArray(balance);
        } catch (Exception e) {
            log.error("getOpe8AssetBalance error...", e);
        }
        return balanceArray;
    }

    /**
     * 获取unboundong
     *
     * @param address
     * @return
     */
    public String getUnBoundOng(String address) {
        OntSdk ontSdk = getOntSdk();
        try {
            String unboundOng = ontSdk.nativevm().ong().unboundOng(address);
            return new BigDecimal(unboundOng).divide(ConstantParam.ONT_TOTAL).toPlainString();
        } catch (Exception e) {
            log.error("getUnBoundOng error...", e);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 根据dapp合约hash获取绑定的节点信息
     *
     * @param contractHash
     * @param dappContractHash
     * @return
     */
    public Map getDappBindedNodeInfo(String contractHash, String dappContractHash) {
        Map<String, Object> rsMap = new HashMap<>();
        rsMap.put("node_name", "");
        rsMap.put("node_pubkey", "");
        try {
            OntSdk ontSdk = getOntSdk();

            List paramList = new ArrayList();
            paramList.add("get_binded_node".getBytes());

            List args = new ArrayList();
            args.add(Helper.hexToBytes(dappContractHash));
            paramList.add(args);
            byte[] params = BuildParams.createCodeParamsScript(paramList);

            Transaction tx = ontSdk.vm().makeInvokeCodeTransaction(Helper.reverse(contractHash), null, params, null, 20000, 500);
            Object obj = ontSdk.getConnect().sendRawTransactionPreExec(tx.toHexString());

            String result = ((JSONObject) obj).getString("Result");
            log.info("contracthash:{},bindedNodeinfo:{}", dappContractHash, result);
            if (com.github.ontio.util.Helper.isNotEmptyAndNull(result)) {
                Map map = (Map) BuildParams.deserializeItem(Helper.hexToBytes(result));
                rsMap.put("node_name", new String(Helper.hexToBytes(((String) map.get("node_name")))));
                rsMap.put("node_pubkey", map.get("node_pubkey"));
            }
            return rsMap;
        } catch (Exception e) {
            log.error("dappContractHash:{} getDappBindedNodeInfo error...", dappContractHash, e);
        }
        return rsMap;
    }


    /**
     * 根据dapp合约hash获取绑定的onid和钱包账户
     *
     * @param contractHash
     * @param dappContractHash
     * @return
     */
    public Map getDappBindedOntidAndAccount(String contractHash, String dappContractHash) {
        Map<String, Object> rsMap = new HashMap<>();
        rsMap.put("receive_account", "");
        rsMap.put("ontid", "");
        try {
            OntSdk ontSdk = getOntSdk();

            List paramList = new ArrayList();
            paramList.add("get_binded_dapp".getBytes());

            List args = new ArrayList();
            args.add(Helper.hexToBytes(dappContractHash));
            paramList.add(args);
            byte[] params = BuildParams.createCodeParamsScript(paramList);

            Transaction tx = ontSdk.vm().makeInvokeCodeTransaction(Helper.reverse(contractHash), null, params, null, 20000, 500);
            Object obj = ontSdk.getConnect().sendRawTransactionPreExec(tx.toHexString());

            String result = ((JSONObject) obj).getString("Result");
            log.info("contracthash:{},bindedAccountinfo:{}", dappContractHash, result);
            if (com.github.ontio.util.Helper.isNotEmptyAndNull(result)) {
                Map map = (Map) BuildParams.deserializeItem(Helper.hexToBytes(result));
                rsMap.put("receive_account", Address.parse((String) map.get("receive_account")).toBase58());
                rsMap.put("ontid", new String(Helper.hexToBytes((String) map.get("ontid"))));
            }
            return rsMap;
        } catch (Exception e) {
            log.error("getDappBindedOntidAndAccount error...", e);
        }
        return rsMap;
    }


    private OntSdk getOntSdk() {
        OntSdk wm = OntSdk.getInstance();
        wm.setRestful(paramsConfig.MASTERNODE_RESTFUL_URL);
        return wm;
    }

    private OntSdk getOep8OntSdk(String codeHash) {
        OntSdk wm = OntSdk.getInstance();
        wm.setRestful(paramsConfig.MASTERNODE_RESTFUL_URL);
        wm.neovm().oep8().setContractAddress(codeHash);
        return wm;
    }

    private OntSdk getOep4OntSdk(String codeHash) {
        OntSdk wm = OntSdk.getInstance();
        wm.setRestful(paramsConfig.MASTERNODE_RESTFUL_URL);
        wm.neovm().oep4().setContractAddress(codeHash);
        return wm;
    }

    private OntSdk getOep5OntSdk(String codeHash) {
        OntSdk wm = OntSdk.getInstance();
        wm.setRestful(paramsConfig.MASTERNODE_RESTFUL_URL);
        wm.neovm().oep5().setContractAddress(codeHash);
        return wm;
    }

    public StorageNodeInfoDetail getStorageNodeDetail(String addr) throws Exception {
        OntSdk wm = OntSdk.getInstance();
        wm.setRestful(paramsConfig.MASTERNODE_RESTFUL_URL);
        Address address = Address.decodeBase58(addr);

        String contractAddress = "0000000000000000000000000000000000000008";
        OntSdk sdk = getOntSdk();
        List list = new ArrayList();
        list.add(new Struct().add(address));
        byte[] arg = NativeBuildParams.createCodeParamsScript(list);
        Transaction tx = sdk.vm().buildNativeParams(new Address(Helper.hexToBytes(contractAddress)), "FsNodeQuery", arg, null, 0, 0);

        JSONObject obj = (JSONObject) sdk.getConnect().sendRawTransactionPreExec(tx.toHexString());
        String result = obj.getString("Result");

        StorageNodeInfoDetail detail = new StorageNodeInfoDetail();

        ByteArrayInputStream bais = new ByteArrayInputStream(Helper.hexToBytes(result));
        BinaryReader reader = new BinaryReader(bais);

        boolean resultBoolean = reader.readBoolean();

        byte[] noedeInfoBytes = reader.readVarBytes();

        ByteArrayInputStream data = new ByteArrayInputStream(noedeInfoBytes);
        BinaryReader br = new BinaryReader(data);

        byte[] pledgeBytes = br.readVarBytes();
        BigInteger pledge = Helper.BigIntFromNeoBytes(pledgeBytes);
        detail.setPledge(pledge);

        byte[] profitBytes = br.readVarBytes();
        BigInteger profit = Helper.BigIntFromNeoBytes(profitBytes);
        detail.setProfit(profit);

        BigDecimal divideUnit = new BigDecimal(1024);
        byte[] volumeBytes = br.readVarBytes();
        BigInteger volume = Helper.BigIntFromNeoBytes(volumeBytes);
        BigDecimal volumeDecimal = new BigDecimal(volume.toString()).divide(divideUnit, 2, BigDecimal.ROUND_DOWN).divide(divideUnit, 2, BigDecimal.ROUND_DOWN);
        detail.setVolume(volumeDecimal.toPlainString() + "GB");

        byte[] restVolBytes = br.readVarBytes();
        BigInteger restVol = Helper.BigIntFromNeoBytes(restVolBytes);
        BigDecimal restVolDecimal = new BigDecimal(restVol.toString()).divide(divideUnit, 2, BigDecimal.ROUND_DOWN).divide(divideUnit, 2, BigDecimal.ROUND_DOWN);
        detail.setRestVol(restVolDecimal.toPlainString() + "GB");

        byte[] serviceTimeBytes = br.readVarBytes();
        BigInteger serviceTime = Helper.BigIntFromNeoBytes(serviceTimeBytes);
        detail.setServiceTime(serviceTime);

        byte[] minPdpIntervalBytes = br.readVarBytes();
        BigInteger minPdpInter = Helper.BigIntFromNeoBytes(minPdpIntervalBytes);
        detail.setMinPdpInterval(minPdpInter);

        byte[] nodeAddrBytes = br.readVarBytes();
        Address nodeAddr = Address.parse(Helper.toHexString(nodeAddrBytes));
        byte[] nodeNetAddr = br.readVarBytes();
        String ip = new String(nodeNetAddr);
        return detail;
    }

}
