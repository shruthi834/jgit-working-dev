package com.feature.JGit.entity;

public class DraftMap {
    Integer id;
    String username;
    String mapName;
    String createDate;

    public DraftMap(String username, String mapName, String createDate) {
        this.id = id;
        this.username = username;
        this.mapName = mapName;
        this.createDate = createDate;
    }

    public Integer getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getMapName() {
        return mapName;
    }

    public String getCreateDate() {
        return createDate;
    }
}
