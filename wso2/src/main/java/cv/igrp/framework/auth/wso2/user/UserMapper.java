package cv.igrp.framework.auth.wso2.user;

import cv.igrp.framework.auth.core.model.UserIdentity;
import cv.igrp.framework.auth.wso2.user.dto.User;
import cv.igrp.framework.auth.wso2.user.dto.Wso2UserResponse;

import java.util.List;

public class UserMapper {

    public static UserIdentity userToUserIdentity(Wso2UserResponse userResponse) {
        List<String> emails = userResponse.getEmails();
        return User.Builder.newBuilder()
                .id(userResponse.getId())
                .userName(userResponse.getUserName())
                .firstName(userResponse.getName().getGivenName())
                .lastName(userResponse.getName().getFamilyName())
                .emails(emails)
                .build();
    }
}
