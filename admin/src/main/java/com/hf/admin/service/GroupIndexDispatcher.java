package com.hf.admin.service;

import com.hf.admin.model.UserGroupRequest;
import com.hf.admin.rpc.AdminClient;
import com.hf.base.dispatcher.DispatchResult;
import com.hf.base.dispatcher.Dispatcher;
import com.hf.base.model.UserGroup;
import com.hf.base.utils.Pagenation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

@Service
public class GroupIndexDispatcher implements Dispatcher {
    @Autowired
    private AdminClient adminClient;

    @Override
    public DispatchResult dispatch(HttpServletRequest request, String page) {
        String companyId = request.getSession().getAttribute("groupId").toString();
        UserGroupRequest userGroupRequest = new UserGroupRequest();
        userGroupRequest.setCompanyId(companyId);
        userGroupRequest.setPageSize(15);
        Pagenation<UserGroup> pagenation = adminClient.getUserGroupList(userGroupRequest);
        DispatchResult dispatchResult = new DispatchResult();
        dispatchResult.setPage(page);
        dispatchResult.addObject("pageInfo",pagenation);
        return dispatchResult;
    }
}
