package com.arextest.config.repository.impl;

import com.arextest.config.mapper.AppMapper;
import com.arextest.config.model.dao.config.AppCollection;
import com.arextest.config.model.dto.application.ApplicationConfiguration;
import com.arextest.config.repository.ConfigRepositoryProvider;
import com.arextest.config.utils.MongoHelper;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UpdateManyModel;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;

public class ApplicationConfigurationRepositoryImpl implements
    ConfigRepositoryProvider<ApplicationConfiguration> {

  private static final String UNKNOWN_APP_NAME = "unknown app name";
  private static final String DOT_OP = ".";
  private final MongoTemplate mongoTemplate;

  @Resource
  private List<ConfigRepositoryProvider> configRepositoryProviders;

  public ApplicationConfigurationRepositoryImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  public MongoCollection<AppCollection> getCollection() {
    return mongoTemplate.getMongoDatabaseFactory().getMongoDatabase()
        .getCollection(AppCollection.DOCUMENT_NAME, AppCollection.class);
  }

  @PostConstruct
  private void init() {
    // flush appName
    flushAppName();
  }

  private void flushAppName() {
    Bson filter = Filters.in(AppCollection.Fields.appName, UNKNOWN_APP_NAME, "", null);
    List<WriteModel<AppCollection>> bulkUpdateOps = new ArrayList<>();
    try (MongoCursor<AppCollection> cursor = getCollection().find(filter).iterator()) {
      while (cursor.hasNext()) {
        AppCollection document = cursor.next();
        document.setAppName(document.getAppId());

        Bson filter2 = Filters.eq(DASH_ID, new ObjectId(document.getId()));
        Bson update = Updates.combine(Arrays.asList(MongoHelper.getUpdate(),
            MongoHelper.getSpecifiedProperties(document, AppCollection.Fields.appName)));
        bulkUpdateOps.add(new UpdateManyModel<>(filter2, update));
      }
    }
    if (bulkUpdateOps.size() > 0) {
      BulkWriteResult result = getCollection().bulkWrite(bulkUpdateOps);
    }
  }

  @Override
  public List<ApplicationConfiguration> list() {

    Bson sort = Sorts.descending(DASH_ID);
    List<ApplicationConfiguration> applicationConfigurations = new ArrayList<>();
    try (MongoCursor<AppCollection> cursor = getCollection().find().sort(sort).iterator()) {
      while (cursor.hasNext()) {
        AppCollection document = cursor.next();
        ApplicationConfiguration dto = AppMapper.INSTANCE.dtoFromDao(document);
        applicationConfigurations.add(dto);
      }
    }
    return applicationConfigurations;
  }

  @Override
  public List<ApplicationConfiguration> listBy(String appId) {

    Bson filter = Filters.eq(AppCollection.Fields.appId, appId);
    List<ApplicationConfiguration> applicationConfigurations = new ArrayList<>();
    try (MongoCursor<AppCollection> cursor = getCollection().find(filter).iterator()) {
      while (cursor.hasNext()) {
        AppCollection document = cursor.next();
        ApplicationConfiguration dto = AppMapper.INSTANCE.dtoFromDao(document);
        applicationConfigurations.add(dto);
      }
    }
    return applicationConfigurations;
  }

  @Override
  public boolean update(ApplicationConfiguration configuration) {

    Bson filter = Filters.eq(AppCollection.Fields.appId, configuration.getAppId());

    List<Bson> updateList = Arrays.asList(MongoHelper.getUpdate(),
        MongoHelper.getSpecifiedProperties(configuration,
            AppCollection.Fields.agentVersion,
            AppCollection.Fields.agentExtVersion,
            AppCollection.Fields.status,
            AppCollection.Fields.features,
            AppCollection.Fields.appName,
            AppCollection.Fields.owners,
            AppCollection.Fields.visibilityLevel,
            AppCollection.Fields.tags));
    Bson updateCombine = Updates.combine(updateList);

    return getCollection().updateMany(filter, updateCombine).getModifiedCount() > 0;
  }

  @Override
  public boolean remove(ApplicationConfiguration configuration) {
    if (StringUtils.isBlank(configuration.getAppId())) {
      return false;
    }
    return this.removeByAppId(configuration.getAppId());
  }

  @Override
  public boolean removeByAppId(String appId) {
    for (ConfigRepositoryProvider configRepositoryProvider : configRepositoryProviders) {
      configRepositoryProvider.removeByAppId(appId);
    }
    Bson filter = Filters.eq(AppCollection.Fields.appId, appId);
    DeleteResult deleteResult = getCollection().deleteMany(filter);
    return deleteResult.getDeletedCount() > 0;
  }

  @Override
  public boolean insert(ApplicationConfiguration configuration) {
    AppCollection appCollection = AppMapper.INSTANCE.daoFromDto(configuration);
    InsertOneResult insertOneResult = getCollection().insertOne(appCollection);
    return insertOneResult.getInsertedId() != null;
  }

  public boolean addEnvToApp(String appId, Map<String, String> tags) {
    if (StringUtils.isBlank(appId) || tags == null || tags.isEmpty()) {
      return false;
    }
    List<Bson> updateList = new ArrayList<>();
    Bson filter = Filters.eq(AppCollection.Fields.appId, appId);
    for (Map.Entry<String, String> entry : tags.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      if (StringUtils.isBlank(value)) {
        continue;
      }
      Bson update = Updates.addToSet(AppCollection.Fields.tags + DOT_OP + key, value);
      updateList.add(update);
    }
    if (updateList.isEmpty()) {
      return false;
    }
    return getCollection().updateOne(filter, Updates.combine(updateList)).getModifiedCount() > 0;
  }

}
