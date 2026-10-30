package com.talhanation.workers.world;

import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Who may trade with whom between merchants: never with the own markets, never between enemies,
 * never against an embargo (recruits stores embargoes per player, so both owners are checked).
 */
public class MerchantTradeRules {

    public static boolean canTrade(@Nullable UUID visitorOwner, @Nullable String visitorTeam, @Nullable UUID hostOwner, @Nullable String hostTeam) {
        if (visitorOwner == null || hostOwner == null) return false;
        if (visitorOwner.equals(hostOwner)) return false;

        RecruitsDiplomacyManager diplomacy = FactionEvents.recruitsDiplomacyManager;
        if (diplomacy == null) return true;

        boolean visitorInTeam = visitorTeam != null && !visitorTeam.isEmpty();
        boolean hostInTeam = hostTeam != null && !hostTeam.isEmpty();

        if (visitorInTeam && hostInTeam && !visitorTeam.equals(hostTeam)) {
            if (diplomacy.getRelation(visitorTeam, hostTeam) == RecruitsDiplomacyManager.DiplomacyStatus.ENEMY) return false;
            if (diplomacy.getRelation(hostTeam, visitorTeam) == RecruitsDiplomacyManager.DiplomacyStatus.ENEMY) return false;
        }
        if (hostInTeam && diplomacy.hasEmbargo(visitorOwner, hostTeam)) return false;
        if (visitorInTeam && diplomacy.hasEmbargo(hostOwner, visitorTeam)) return false;
        return true;
    }
}
