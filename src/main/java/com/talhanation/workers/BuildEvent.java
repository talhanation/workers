package com.talhanation.workers;

import com.talhanation.workers.entities.BuilderEntity;
import com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity;
import com.talhanation.workers.entities.workarea.BuildArea;
import net.neoforged.bus.api.Event;

import java.util.List;

/**
 * Build events on the {@code NeoForge.EVENT_BUS}, server-side only.
 * Meant for the Governor compat: he reacts when one of his buildings is finished instead of checking every tick.
 */
public abstract class BuildEvent extends Event {

    private final BuildArea buildArea;

    protected BuildEvent(BuildArea buildArea) {
        this.buildArea = buildArea;
    }

    public BuildArea getBuildArea() {
        return buildArea;
    }

    /**
     * Fired when a build was started: Build pressed, a paid build loaded again, or started by the Governor.
     * The status is WAITING_FOR_BUILDER until a builder takes it.
     */
    public static class Started extends BuildEvent {
        public Started(BuildArea buildArea) {
            super(buildArea);
        }
    }

    /**
     * Fired when the status of a build changes, e.g. to WAITING_FOR_MATERIALS
     * (then {@link BuildArea#getMissingMaterials()} has the materials) or back to BUILDING.
     */
    public static class StatusChanged extends BuildEvent {
        private final BuildArea.BuildStatus oldStatus;
        private final BuildArea.BuildStatus newStatus;

        public StatusChanged(BuildArea buildArea, BuildArea.BuildStatus oldStatus, BuildArea.BuildStatus newStatus) {
            super(buildArea);
            this.oldStatus = oldStatus;
            this.newStatus = newStatus;
        }

        public BuildArea.BuildStatus getOldStatus() {
            return oldStatus;
        }

        public BuildArea.BuildStatus getNewStatus() {
            return newStatus;
        }
    }

    /** Fired once when a builder placed the last block. The build area is removed right after. */
    public static class Finished extends BuildEvent {
        private final BuilderEntity builder;
        private final List<AbstractWorkAreaEntity> workAreas;

        public Finished(BuildArea buildArea, BuilderEntity builder, List<AbstractWorkAreaEntity> workAreas) {
            super(buildArea);
            this.builder = builder;
            this.workAreas = workAreas;
        }

        public BuilderEntity getBuilder() {
            return builder;
        }

        /** Work areas spawned from the template, empty if the build area was set to build without them. */
        public List<AbstractWorkAreaEntity> getWorkAreas() {
            return workAreas;
        }
    }
}
