package com.hf.core.biz.service.impl;

import com.hf.base.enums.OprStatus;
import com.hf.base.enums.OprType;
import com.hf.base.enums.SettleStatus;
import com.hf.base.exceptions.BizFailException;
import com.hf.base.model.AgentPayLog;
import com.hf.core.biz.service.SettleService;
import com.hf.core.dao.local.*;
import com.hf.core.model.po.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Objects;

@Service
public class SettleServiceImpl implements SettleService {
    @Autowired
    private SettleTaskDao settleTaskDao;
    @Autowired
    private UserChannelAccountDao userChannelAccountDao;
    @Autowired
    private AccountOprLogDao accountOprLogDao;
    @Autowired
    private AccountDao accountDao;
    @Autowired
    private AdminAccountOprLogDao adminAccountOprLogDao;
    @Autowired
    private AdminAccountDao adminAccountDao;

    @Transactional
    @Override
    public void agentPay(SettleTask settleTask, List<AgentPayLog> logs) {

    }

    @Override
    public void agentPaySuccess() {

    }

    @Transactional
    @Override
    public void agentPayFailed(Long logId) {
//        AgentPayLog agentPayLog = agentPayLogDao.selectByPrimaryKey(logId);
//        int count = agentPayLogDao.updateStatus(agentPayLog.getId(), SettleStatus.NEW.getValue(),SettleStatus.FAILED.getValue());
//        if(count<=0) {
//            throw new BizFailException("update agent pay log status failed");
//        }
//        SettleTask settleTask = settleTaskDao.selectByPrimaryKey(agentPayLog.getWithDrawTaskId());
//        count = settleTaskDao.unlock(settleTask.getId(),agentPayLog.getAmount(),settleTask.getVersion());
//        if(count<=0) {
//            throw new BizFailException("unlock task failed");
//        }
//        UserChannelAccount userChannelAccount = userChannelAccountDao.selectByUnq(settleTask.getGroupId(),agentPayLog.getProviderCode());
//        count = userChannelAccountDao.unlock(userChannelAccount.getId(),agentPayLog.getAmount(),userChannelAccount.getVersion());
//        if(count<=0) {
//            throw new BizFailException("unlock account failed");
//        }
    }

    @Transactional
    @Override
    public void paySuccess(SettleTask settleTask, AccountOprLog accountOprLog) {
        settleTask = settleTaskDao.selectByPrimaryKey(settleTask.getId());
        //支付金额
        int count = accountOprLogDao.updateStatusById(accountOprLog.getId(), OprStatus.NEW.getValue(),OprStatus.FINISHED.getValue());
        if(count<=0) {
            throw new BizFailException("update oprLog status failed");
        }

        UserChannelAccount userChannelAccount = userChannelAccountDao.selectByUnq(settleTask.getGroupId(),accountOprLog.getProviderCode());
        count = userChannelAccountDao.finish(userChannelAccount.getId(),accountOprLog.getAmount(),userChannelAccount.getVersion());
        if(count<=0) {
            throw new BizFailException("finish user channel account failed");
        }

        Account withdrawAccount = accountDao.selectByPrimaryKey(settleTask.getAccountId());
        count = accountDao.finishWithDraw(withdrawAccount.getId(),accountOprLog.getAmount(),withdrawAccount.getVersion());
        if(count<=0) {
            throw new BizFailException("finish withdraw failed");
        }

        count = settleTaskDao.finish(settleTask.getId(),accountOprLog.getAmount(),settleTask.getVersion());
        if(count<=0) {
            throw new BizFailException("finish settle task failed");
        }

        settleTask = settleTaskDao.selectByPrimaryKey(settleTask.getId());
        if(settleTask.getPayAmount().compareTo(settleTask.getPaidAmount())<=0) {
            //支付完成
            finishFee(settleTask);
            finishAdmin(settleTask);
            settleTask = settleTaskDao.selectByPrimaryKey(settleTask.getId());
            if(settleTask.getSettleAmount().compareTo(settleTask.getPaidAmount())==0) {
                settleTaskDao.updateStatusById(settleTask.getId(),SettleStatus.PROCESSING.getValue(),SettleStatus.SUCCESS.getValue());
            }
        }
    }

    private void finishFee(SettleTask settleTask) {
        List<AccountOprLog> taxLogs = accountOprLogDao.selectByUnq(String.valueOf(settleTask.getId()),settleTask.getGroupId(),OprType.TAX.getValue());
        int count = 0;
        for(AccountOprLog taxLog:taxLogs) {
            count = accountOprLogDao.updateStatusById(taxLog.getId(),OprStatus.NEW.getValue(),OprStatus.FINISHED.getValue());
            if(count<=0) {
                throw new BizFailException("update taxLog status failed");
            }
            Account withdrawAccount = accountDao.selectByPrimaryKey(settleTask.getAccountId());
            count = accountDao.finishTax(withdrawAccount.getId(),taxLog.getAmount(),withdrawAccount.getVersion());
            if(count<=0) {
                throw new BizFailException("update fee account failed");
            }
            UserChannelAccount userChannelAccount = userChannelAccountDao.selectByUnq(settleTask.getGroupId(),taxLog.getProviderCode());
            count = userChannelAccountDao.finish(userChannelAccount.getId(),taxLog.getAmount(),userChannelAccount.getVersion());
            if(count<=0) {
                throw new BizFailException("finish user channel account failed");
            }
            settleTask = settleTaskDao.selectByPrimaryKey(settleTask.getId());
            count = settleTaskDao.finish(settleTask.getId(),taxLog.getAmount(),settleTask.getVersion());
            if(count<=0) {
                throw new BizFailException("update settle task paid amount failed");
            }
        }

        List<AccountOprLog> feeLogs = accountOprLogDao.selectByUnq(String.valueOf(settleTask.getId()),settleTask.getPayGroupId(),OprType.FEE.getValue());
        for(AccountOprLog feeLog:feeLogs) {
            count = accountOprLogDao.updateStatusById(feeLog.getId(),OprStatus.NEW.getValue(),OprStatus.FINISHED.getValue());
            if(count<=0) {
                throw new BizFailException("update feeLog status failed");
            }
            Account payAccount = accountDao.selectByGroupId(settleTask.getPayGroupId());
            count = accountDao.addAmount(payAccount.getId(),feeLog.getAmount(),payAccount.getVersion());
            if(count<=0) {
                throw new BizFailException("update pay Account failed");
            }
            UserChannelAccount userChannelAccount = userChannelAccountDao.selectByUnq(payAccount.getGroupId(),feeLog.getProviderCode());
            count = userChannelAccountDao.addAmount(userChannelAccount.getId(),feeLog.getAmount(),userChannelAccount.getVersion());
            if(count<=0) {
                throw new BizFailException("update pay user account failed");
            }
        }
    }

    private void finishAdmin(SettleTask settleTask) {
        AdminAccountOprLog adminLog = adminAccountOprLogDao.selectByNo(String.valueOf(settleTask.getId()));
        if(adminLog.getType() != OprType.WITHDRAW.getValue()) {
            throw new BizFailException("opr log type not match");
        }
        //admin账户
        int count = adminAccountOprLogDao.updateStatusById(adminLog.getId(),OprStatus.NEW.getValue(),OprStatus.FINISHED.getValue());
        if(count<=0) {
            throw new BizFailException("update admin Opr log status failed");
        }

        AdminAccount adminAccount = adminAccountDao.selectByGroupId(settleTask.getPayGroupId());
        count = adminAccountDao.finishPay(adminAccount.getId(),adminLog.getAmount(),adminAccount.getVersion());
        if(count<=0) {
            throw new BizFailException("update admin amount failed");
        }
    }

    @Transactional
    @Override
    public void adminPaySuccess(SettleTask settleTask, AccountOprLog accountOprLog) {
        if(accountOprLog.getType() == OprType.WITHDRAW.getValue()) {
            int count = accountOprLogDao.updateStatusById(accountOprLog.getId(),OprStatus.NEW.getValue(),OprStatus.FINISHED.getValue());
            if(count<=0) {
                throw new BizFailException("update account opr log status failed");
            }
            Account account = accountDao.selectByGroupId(accountOprLog.getGroupId());
            count = accountDao.finishWithDraw(account.getId(),accountOprLog.getAmount(),account.getVersion());
            if(count<=0) {
                throw new BizFailException("update account failed");
            }
            UserChannelAccount userChannelAccount = userChannelAccountDao.selectByUnq(accountOprLog.getGroupId(),accountOprLog.getProviderCode());
            count = userChannelAccountDao.finish(userChannelAccount.getId(),accountOprLog.getAmount(),userChannelAccount.getVersion());
            if(count<=0) {
                throw new BizFailException("update user channel Account failed");
            }
            AdminAccount adminAccount = adminAccountDao.selectByGroupId(accountOprLog.getGroupId());
            count = adminAccountDao.finishPay(adminAccount.getId(),accountOprLog.getAmount(),adminAccount.getVersion());
            if(count<=0) {
                throw new BizFailException("update admin account failed");
            }
            settleTask = settleTaskDao.selectByPrimaryKey(settleTask.getId());
            count = settleTaskDao.finish(settleTask.getId(),accountOprLog.getAmount(),settleTask.getVersion());
            if(count<=0) {
                throw new BizFailException("update settle task failed");
            }
        }

        if(accountOprLog.getType() == OprType.ADMIN_WITHDRAW.getValue()) {
            int count = accountOprLogDao.updateStatusById(accountOprLog.getId(),OprStatus.NEW.getValue(),OprStatus.FINISHED.getValue());
            if(count<=0) {
                throw new BizFailException("update account opr log status failed");
            }
            UserChannelAccount userChannelAccount = userChannelAccountDao.selectByUnq(accountOprLog.getAccountId(),accountOprLog.getProviderCode());
            count = userChannelAccountDao.finish(userChannelAccount.getId(),accountOprLog.getAmount(),userChannelAccount.getVersion());
            if(count<=0) {
                throw new BizFailException("update user channel Account failed");
            }

            UserChannelAccount jhChannelAccount = userChannelAccountDao.selectByUnq(accountOprLog.getAccountId(),"jh");
            if(Objects.isNull(jhChannelAccount)) {
                Account account = accountDao.selectByGroupId(accountOprLog.getAccountId());
                jhChannelAccount = new UserChannelAccount();
                jhChannelAccount.setGroupId(accountOprLog.getAccountId());
                jhChannelAccount.setChannelProvider("jh");
                jhChannelAccount.setAccountId(account.getId());
                userChannelAccountDao.insertSelective(jhChannelAccount);
                jhChannelAccount = userChannelAccountDao.selectByUnq(accountOprLog.getAccountId(),"jh");
            }
            count = userChannelAccountDao.addAmount(jhChannelAccount.getId(),accountOprLog.getAmount(),jhChannelAccount.getVersion());
            if(count<=0) {
                throw new BizFailException("update channel account failed");
            }

            settleTask = settleTaskDao.selectByPrimaryKey(settleTask.getId());
            count = settleTaskDao.finish(settleTask.getId(),accountOprLog.getAmount(),settleTask.getVersion());
            if(count<=0) {
                throw new BizFailException("update settle task failed");
            }
        }

        settleTask = settleTaskDao.selectByPrimaryKey(settleTask.getId());
        if(settleTask.getSettleAmount().compareTo(settleTask.getPaidAmount())==0) {
            settleTaskDao.updateStatusById(settleTask.getId(),SettleStatus.PROCESSING.getValue(),SettleStatus.SUCCESS.getValue());
        }
    }
}