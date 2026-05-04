package com.talhanation.workers.entities.workarea;

import java.util.UUID;

public interface IPermissionArea {

    UUID getPlayerUUID();

    boolean getTeamAccess();
    String getTeamStringID();
}
