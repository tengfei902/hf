package com.hf.core.biz.trade;

import com.google.gson.Gson;
import com.hf.base.contants.CodeManager;
import com.hf.base.enums.*;
import com.hf.base.enums.ChannelProvider;
import com.hf.base.exceptions.BizFailException;
import com.hf.base.utils.EpaySignUtil;
import com.hf.base.utils.MapUtils;
import com.hf.base.utils.Utils;
import com.hf.core.biz.service.CacheService;
import com.hf.core.biz.service.PayService;
import com.hf.core.dao.local.PayRequestDao;
import com.hf.core.dao.local.UserGroupDao;
import com.hf.core.dao.local.UserGroupExtDao;
import com.hf.core.dao.remote.CallBackClient;
import com.hf.core.dao.remote.PayClient;
import com.hf.core.model.PropertyConfig;
import com.hf.core.model.dto.trade.unifiedorder.WwPayRequest;
import com.hf.core.model.po.*;
import com.hf.core.utils.CipherUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class WwTradingBiz extends AbstractTradingBiz {
    @Autowired
    private CacheService cacheService;
    @Autowired
    private UserGroupExtDao userGroupExtDao;
    @Autowired
    private PropertyConfig propertyConfig;
    @Autowired
    private PayClient wwClient;
    @Autowired
    private PayService payService;
    @Autowired
    private PayRequestDao payRequestDao;
    @Autowired
    private UserGroupDao userGroupDao;
    @Autowired
    private CallBackClient callBackClient;

    private static final String[] callbackFields = {"memberCode","orderNum","payNum","payType","payMoney","payTime","platformType","interfaceType","respType","resultCode","resultMsg","signStr"};

    @Override
    public ChannelProvider getChannelProvider() {
        return ChannelProvider.WW;
    }

    @Override
    public void doPay(PayRequest payRequest) {
        UserGroup userGroup = cacheService.getGroup(payRequest.getMchId());
        ChannelProvider channelProvider = ChannelProvider.parse(payRequest.getChannelProviderCode());
        UserGroupExt userGroupExt = userGroupExtDao.selectByUnq(userGroup.getId(),channelProvider.getCode());

        WwPayRequest wwPayRequest = new WwPayRequest();
        ChannelCode channelCode = ChannelCode.parseFromCode(payRequest.getService());
        if(channelCode == ChannelCode.WX_H5) {
            wwPayRequest.setMemberCode(userGroupExt.getMerchantNo());
            wwPayRequest.setOrderNum(payRequest.getOutTradeNo());
            wwPayRequest.setPayMoney(String.valueOf(new BigDecimal(payRequest.getTotalFee()).divide(new BigDecimal("100"),2,BigDecimal.ROUND_HALF_UP)));
            wwPayRequest.setPayType("1");
            wwPayRequest.setSceneInfo("慧富微信H5支付");
            wwPayRequest.setIp(payRequest.getCreateIp());
            wwPayRequest.setCallbackUrl(propertyConfig.getWwCallbackUrl());
            wwPayRequest.setChannelCode(payRequest.getService());
        } else if(channelCode == ChannelCode.QQ_H5) {
            wwPayRequest.setMemberCode(userGroupExt.getMerchantNo());
            wwPayRequest.setOrderNum(payRequest.getOutTradeNo());
            wwPayRequest.setPayMoney(String.valueOf(new BigDecimal(payRequest.getTotalFee()).divide(new BigDecimal("100"),2,BigDecimal.ROUND_HALF_UP)));
            wwPayRequest.setPayType("3");
            wwPayRequest.setSceneInfo("慧富QQH5支付");
            wwPayRequest.setIp(payRequest.getCreateIp());
            wwPayRequest.setCallbackUrl(propertyConfig.getWwCallbackUrl());
            wwPayRequest.setChannelCode(payRequest.getService());
        } else if(channelCode == ChannelCode.WY) {
            wwPayRequest.setCallbackUrl(propertyConfig.getWwCallbackUrl());
            wwPayRequest.setMemberCode(userGroupExt.getMerchantNo());
            wwPayRequest.setOrderNum(payRequest.getOutTradeNo());
            wwPayRequest.setPayMoney(String.valueOf(new BigDecimal(payRequest.getTotalFee()).divide(new BigDecimal("100"),2,BigDecimal.ROUND_HALF_UP)));
            wwPayRequest.setChannelCode(payRequest.getService());
            if(!Utils.isEmpty(payRequest.getBankCode())) {
                wwPayRequest.setBankCode(payRequest.getBankCode());
            }
            if(!Utils.isEmpty(payRequest.getRemark())) {
                wwPayRequest.setGoodsName(payRequest.getRemark());
            }
        } else {
            throw new BizFailException("no channel defined");
        }

        Map<String,Object> response = wwClient.unifiedorder(MapUtils.beanToMap(wwPayRequest));
        PayRequestBack payRequestBack = new PayRequestBack();
        payRequestBack.setMchId(payRequest.getMchId());
        payRequestBack.setOutTradeNo(payRequest.getOutTradeNo());
        String payContent = String.valueOf(response.get("payResult"));

        switch (channelCode) {
            case WX_H5:
            case QQ_H5:
                if(StringUtils.isEmpty(payContent) || !payContent.contains("var url = ")) {
                    String payResult = response.get("payResult") == null?"":String.valueOf(response.get("payResult"));
                    try {
                        String message = payResult.substring(payResult.indexOf("<span>")+6,payResult.lastIndexOf("</span>")).replace("<b>","").replace("</b>","").replace("\n","").replace("<p>","").replace("</p>","").replace("，",",").replace("\t","").replace("<span>","").replace("</span>","").replace("\r","");
                        payRequestBack.setMessage(message);
                    } catch (Exception e) {
                        payRequestBack.setMessage(payResult);
                    }

                    payRequestBack.setErrcode(CodeManager.BIZ_FAIELD);
                    payService.payFailed(payRequest.getOutTradeNo(),payRequestBack);
                    return;
                }
                payRequestBack.setErrcode(CodeManager.PAY_SUCCESS);
                payRequestBack.setMessage("下单成功");
                String codeUrl = payContent.split("'")[1];
                payRequestBack.setCodeUrl(codeUrl);
                break;
            case WY:
                if(StringUtils.isEmpty(payContent) || !payContent.contains("pGateWayReq")) {
                    String payResult = response.get("payResult") == null?"":String.valueOf(response.get("payResult"));
                    try {
                        String message = payResult.substring(payResult.indexOf("<span>")+6,payResult.lastIndexOf("</span>")).replace("<b>","").replace("</b>","").replace("\n","").replace("<p>","").replace("</p>","").replace("，",",").replace("\t","").replace("<span>","").replace("</span>","").replace("\r","");
                        payRequestBack.setMessage(message);
                    } catch (Exception e) {
                        payRequestBack.setMessage(payResult);
                    }
                    payRequestBack.setErrcode(CodeManager.PAY_FAILED);
                    payService.payFailed(payRequest.getOutTradeNo(),payRequestBack);
                    return;
                }
                payRequestBack.setErrcode(CodeManager.PAY_SUCCESS);
                payRequestBack.setMessage("下单成功");
                String content = payContent.substring(payContent.indexOf("<form"),payContent.indexOf("</form>")+7);
                payRequestBack.setPageContent(content);
                break;
            default:
        }

        payRequest = payRequestDao.selectByTradeNo(payRequest.getOutTradeNo());
        payService.remoteSuccess(payRequest,payRequestBack);
    }

    @Override
    public String handleCallBack(Map<String, String> map) {
        for(String field:callbackFields) {
            if(Objects.isNull(map.get(field)) || Utils.isEmpty(String.valueOf(map.get(field)))) {
                throw new BizFailException(String.format("%s不能为空",field));
            }
        }
        String memberCode = map.get("memberCode");
        String orderNum = map.get("orderNum");
        String payNum = map.get("payNum");
        int payType = new BigDecimal(map.get("payType")).intValue();
        BigDecimal payMoney = new BigDecimal(map.get("payMoney"));
        String payTime = map.get("payTime");
        int platformType = new BigDecimal(map.get("platformType")).intValue();
        int interfaceType = new BigDecimal(map.get("interfaceType")).intValue();
        int respType = new BigDecimal(map.get("respType")).intValue();
        String resultMsg = map.get("resultMsg");
        String signStr = map.get("signStr");

        PayRequest payRequest = payRequestDao.selectByTradeNo(orderNum);
        PayRequestStatus payRequestStatus = PayRequestStatus.parse(payRequest.getStatus()) ;

        if(payRequestStatus == PayRequestStatus.OPR_SUCCESS) {
            return new Gson().toJson(MapUtils.buildMap("resCode","0000"));
        }

        if(payRequestStatus != PayRequestStatus.PROCESSING && payRequestStatus!=PayRequestStatus.OPR_GENERATED) {
            throw new BizFailException("status invalid");
        }

        if(respType == 1) {
            return new Gson().toJson(MapUtils.buildMap("resCode","0000"));
        }

        if(respType == 2) {
            payService.paySuccess(orderNum);
        }

        if(respType == 3) {
            payService.payFailed(orderNum);
        }

        return new Gson().toJson(MapUtils.buildMap("resCode","0000"));
    }

    @Override
    public Map<String, Object> query(PayRequest payRequest) {
        UserGroup userGroup = cacheService.getGroup(payRequest.getMchId());
        UserGroupExt userGroupExt = userGroupExtDao.selectByUnq(userGroup.getId(), ChannelProvider.WW.getCode());
        Map<String,Object> params = new HashMap<>();
        params.put("memberCode",userGroupExt.getMerchantNo());
        params.put("orderNum",payRequest.getOutTradeNo());
        String signUrl = Utils.getEncryptStr(params);
        String signStr = EpaySignUtil.sign(CipherUtils.private_key,signUrl);
        params.put("signStr",signStr);
        Map<String,Object> resultMap = wwClient.orderinfo(params);
        if(org.apache.commons.collections.MapUtils.isEmpty(resultMap)) {
            resultMap.put("errcode",99);
            throw new BizFailException("result is null");
        }
        resultMap.put("errcode",0);
        String orderCode = String.valueOf(resultMap.get("orderCode"));
        String oriRespCode = String.valueOf(resultMap.get("oriRespCode"));
        String returnCode = String.valueOf(resultMap.get("returnCode"));
        String oriRespMsg = String.valueOf(resultMap.get("oriRespMsg"));

        resultMap.put("out_trade_no",orderCode);
        resultMap.put("message",oriRespMsg);

        logger.warn(String.format("%s query ww status : %s,%s",payRequest.getOutTradeNo(),oriRespCode,returnCode));

//        上游无失败，只有成功
        if(!StringUtils.equals(returnCode,"0000")) {
            logger.warn(String.format("%s query failed,%s",payRequest.getOutTradeNo(),new Gson().toJson(resultMap)));
            throw new BizFailException("retry");
        }

        if(StringUtils.equals(oriRespCode,"555555")) {
            throw new BizFailException("retry");
        }

        if(StringUtils.equals(oriRespCode,"000000")) {
            resultMap.put("status",1);
        } else {
            resultMap.put("status",0);
        }
//        else {
//            resultMap.put("status",5);
//        }
        return resultMap;
    }
}
