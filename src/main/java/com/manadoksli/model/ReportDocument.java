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

@Document(indexName = "reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String imageId;

    @Field(type = FieldType.Keyword)
    private String reason;

    @Field(type = FieldType.Date)
    private OffsetDateTime reportedAt;
}