package com.acme.ride.dispatch;

import java.util.ArrayList;
import java.util.List;

import org.kie.api.task.UserGroupCallback;

public class SimpleUserGroupCallback implements UserGroupCallback {
    @Override
    public boolean existsUser(String userId) {
        return true;
    }

    @Override
    public boolean existsGroup(String groupId) {
        return true;
    }

    @Override
    public List<String> getGroupsForUser(String userId) {
        return new ArrayList<>();
    }
}
