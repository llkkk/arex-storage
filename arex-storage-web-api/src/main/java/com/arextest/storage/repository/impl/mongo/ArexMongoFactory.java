package com.arextest.storage.repository.impl.mongo;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoDatabase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

/**
 * @author jmo
 * @since 2022/2/17
 */
public class ArexMongoFactory extends SimpleMongoClientDatabaseFactory {
  private final MongoDatabase singleton;

  public ArexMongoFactory(String connectionString) {
    super(connectionString);
    this.singleton = createWithCustomRegistry();

  }

  @Override
  public MongoDatabase getMongoDatabase() {
    return this.singleton;
  }

  protected MongoDatabase createWithCustomRegistry() {
    List<CodecProvider> codecs = ArexCodecFactory.get(super.getMongoDatabase());
    CodecRegistry registry = customCodecRegistry(codecs);
    return super.getMongoDatabase().withCodecRegistry(registry);
  }

  /**
   * any custom item should be first except pojo
   *
   * @return the combinatorial CodecRegistry
   */
  protected static CodecRegistry customCodecRegistry(List<CodecProvider> additionalCodecProviders) {
    List<CodecProvider> codecProviders =
        new ArrayList<>(Optional.ofNullable(additionalCodecProviders).orElse(Collections.emptyList()));

    codecProviders.add(PojoCodecProvider.builder().automatic(true).build());

    CodecRegistry registry = CodecRegistries.fromProviders(codecProviders);
    return CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), registry);
  }
}