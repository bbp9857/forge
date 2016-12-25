package forge.ai.simulation;

import java.util.ArrayList;
import java.util.List;

import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;

public class PossibleTargetSelector {
    private SpellAbility sa;
    private TargetRestrictions tgt;
    private int targetIndex;
    private List<GameObject> validTargets;

    public static class Targets {
        final int originalTargetCount;
        final int targetIndex;
        final String description;
        
        private Targets(int originalTargetCount, int targetIndex, String description)  {
            this.originalTargetCount = originalTargetCount;
            this.targetIndex = targetIndex;
            this.description = description;

            if (targetIndex < 0 || targetIndex >= originalTargetCount) {
                throw new IllegalArgumentException("Invalid targetIndex=" + targetIndex);
            }
        }
        
        @Override
        public String toString() {
            return description;
        }
    }

    public PossibleTargetSelector(Game game, Player self, SpellAbility sa) {
        this.sa = sa;
        this.tgt = sa.getTargetRestrictions();
        this.targetIndex = 0;
        this.validTargets = new ArrayList<GameObject>();
        sa.resetTargets();
        sa.setActivatingPlayer(self);
        for (GameObject o : tgt.getAllCandidates(sa, true)) {
            validTargets.add(o);
        }
    }

    private void selectTargetsByIndex(int index) {
        sa.resetTargets();

        // TODO: smarter about multiple targets, identical targets, etc...
        while (sa.getTargets().getNumTargeted() < tgt.getMaxTargets(sa.getHostCard(), sa) && index < validTargets.size()) {
            sa.getTargets().add(validTargets.get(index++));
        }

        // Divide up counters, since AI is expected to do this. For now,
        // divided evenly with left-overs going to the first target.
        if (sa.hasParam("DividedAsYouChoose")) {
            final int targetCount = sa.getTargets().getTargetCards().size();
            if (targetCount > 0) {
                final String amountStr = sa.getParam("CounterNum");
                final int amount = AbilityUtils.calculateAmount(sa.getHostCard(), amountStr, sa);
                final int amountPerCard = amount / targetCount;
                int amountLeftOver = amount - (amountPerCard * targetCount);
                final TargetRestrictions tgtRes = sa.getTargetRestrictions();
                for (GameObject target : sa.getTargets().getTargets()) {
                    tgtRes.addDividedAllocation(target, amountPerCard + amountLeftOver);
                    amountLeftOver = 0;
                }
            }
        }
    }

    public Targets getLastSelectedTargets() {
        return new Targets(validTargets.size(), targetIndex - 1, sa.getTargets().getTargetedString());
    }

    public boolean selectTargets(Targets targets) {
        if (targets.originalTargetCount != validTargets.size()) {
            return false;
        }
        selectTargetsByIndex(targets.targetIndex);
        return true;
    }
 
    public boolean selectNextTargets() {
        if (targetIndex >= validTargets.size()) {
            return false;
        }
        selectTargetsByIndex(targetIndex);
        targetIndex++;
        return true;
    }
}
