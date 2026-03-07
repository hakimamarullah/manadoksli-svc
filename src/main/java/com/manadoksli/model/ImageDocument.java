package com.manadoksli.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.OffsetDateTime;

@Document(indexName = "images")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageDocument {

    public static final String IMAGES = "images";

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String text;

    @Field(type = FieldType.Keyword)
    private String imageUrl;

    @Field(type = FieldType.Date)
    private OffsetDateTime uploadedAt;
}
