package com.arextest.config.model.dao.config;

import com.arextest.config.model.dao.BaseEntity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldNameConstants;

@Data
@NoArgsConstructor
@FieldNameConstants
public class AppCollection extends BaseEntity {

    public static final String DOCUMENT_NAME = "App";

    @NonNull
    private String appId;

    private int features;

    @NonNull
    private String groupName;
    @NonNull
    private String groupId;

    private String agentVersion;

    private String agentExtVersion;
    @NonNull
    private String appName;

    private String description;
    @NonNull
    private String category;
    @NonNull
    private String owner;
    @NonNull
    private String organizationName;
    @NonNull
    private Integer recordedCaseCount;
    @NonNull
    private String organizationId;
    @NonNull
    private Integer status;

}