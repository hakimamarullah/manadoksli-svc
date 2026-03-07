package com.manadoksli.dto;

import lombok.Data;

@Data
public class SearchReq {


    private String query;

    private int page = 0;
    private int size = 10;
}
