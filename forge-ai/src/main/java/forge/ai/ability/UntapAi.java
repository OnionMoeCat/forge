package forge.ai.ability;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCost;
import forge.ai.SpellAbilityAi;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates.Presets;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;

import java.util.Collection;
import java.util.List;

public class UntapAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final Card source = sa.getHostCard();
        final Cost cost = sa.getPayCosts();

        if (!ComputerUtilCost.checkAddM1M1CounterCost(cost, source)) {
            return false;
        }

        if (!ComputerUtilCost.checkDiscardCost(ai, cost, sa.getHostCard())) {
            return false;
        }

        if (ComputerUtil.preventRunAwayActivations(sa)) {
        	return false;
        }

        if (tgt == null) {
            final List<Card> pDefined = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("Defined"), sa);
            if ((pDefined != null) && pDefined.get(0).isUntapped() && pDefined.get(0).getController() == ai) {
                return false;
            }
        } else {
            if (!untapPrefTargeting(ai, tgt, sa, false)) {
                return false;
            }
        }

        return true;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        final TargetRestrictions tgt = sa.getTargetRestrictions();

        if (tgt == null) {
            if (mandatory) {
                return true;
            }

            // TODO: use Defined to determine, if this is an unfavorable result
            final List<Card> pDefined = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("Defined"), sa);
            if ((pDefined != null) && pDefined.get(0).isUntapped() && pDefined.get(0).getController() == ai) {
                return false;
            }

            return true;
        } else {
            if (untapPrefTargeting(ai, tgt, sa, mandatory)) {
                return true;
            } else if (mandatory) {
                // not enough preferred targets, but mandatory so keep going:
                return untapUnpreferredTargeting(sa, mandatory);
            }
        }

        return false;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        final TargetRestrictions tgt = sa.getTargetRestrictions();

        boolean randomReturn = true;

        if (tgt == null) {
            // who cares if its already untapped, it's only a subability?
        } else {
            if (!untapPrefTargeting(ai, tgt, sa, false)) {
                return false;
            }
        }

        return randomReturn;
    }

    /**
     * <p>
     * untapPrefTargeting.
     * </p>
     * 
     * @param tgt
     *            a {@link forge.game.spellability.TargetRestrictions} object.
     * @param af
     *            a {@link forge.game.ability.AbilityFactory} object.
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean untapPrefTargeting(final Player ai, final TargetRestrictions tgt, final SpellAbility sa, final boolean mandatory) {
        final Card source = sa.getHostCard();

        Player targetController = ai;

        if (sa.isCurse()) {
            targetController = ai.getOpponent();
        }

        List<Card> untapList = targetController.getCardsIn(ZoneType.Battlefield);
        untapList = CardLists.getTargetableCards(untapList, sa);
        untapList = CardLists.getValidCards(untapList, tgt.getValidTgts(), source.getController(), source);

        untapList = CardLists.filter(untapList, Presets.TAPPED);
        // filter out enchantments and planeswalkers, their tapped state doesn't
        // matter.
        final String[] tappablePermanents = { "Creature", "Land", "Artifact" };
        untapList = CardLists.getValidCards(untapList, tappablePermanents, source.getController(), source);

        if (untapList.isEmpty()) {
            return false;
        }

        while (sa.getTargets().getNumTargeted() < tgt.getMaxTargets(sa.getHostCard(), sa)) {
            Card choice = null;

            if (untapList.isEmpty()) {
                if ((sa.getTargets().getNumTargeted() < tgt.getMinTargets(sa.getHostCard(), sa)) || (sa.getTargets().getNumTargeted() == 0)) {
                    sa.resetTargets();
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            if (CardLists.getNotType(untapList, "Creature").isEmpty()) {
                choice = ComputerUtilCard.getBestCreatureAI(untapList); // if only creatures take the best
            } else {
                choice = ComputerUtilCard.getMostExpensivePermanentAI(untapList, sa, false);
            }

            if (choice == null) { // can't find anything left
                if ((sa.getTargets().getNumTargeted() < tgt.getMinTargets(sa.getHostCard(), sa)) || (sa.getTargets().getNumTargeted() == 0)) {
                    sa.resetTargets();
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            untapList.remove(choice);
            sa.getTargets().add(choice);
        }
        return true;
    }

    /**
     * <p>
     * untapUnpreferredTargeting.
     * </p>
     * 
     * @param af
     *            a {@link forge.game.ability.AbilityFactory} object.
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private boolean untapUnpreferredTargeting(final SpellAbility sa, final boolean mandatory) {
        final Card source = sa.getHostCard();
        final TargetRestrictions tgt = sa.getTargetRestrictions();

        List<Card> list = sa.getActivatingPlayer().getGame().getCardsIn(ZoneType.Battlefield);

        list = CardLists.getValidCards(list, tgt.getValidTgts(), source.getController(), source);
        list = CardLists.getTargetableCards(list, sa);

        // filter by enchantments and planeswalkers, their tapped state doesn't
        // matter.
        final String[] tappablePermanents = { "Enchantment", "Planeswalker" };
        List<Card> tapList = CardLists.getValidCards(list, tappablePermanents, source.getController(), source);

        if (untapTargetList(source, tgt, sa, mandatory, tapList)) {
            return true;
        }

        // try to just tap already tapped things
        tapList = CardLists.filter(list, Presets.UNTAPPED);

        if (untapTargetList(source, tgt, sa, mandatory, tapList)) {
            return true;
        }

        // just tap whatever we can
        tapList = list;

        if (untapTargetList(source, tgt, sa, mandatory, tapList)) {
            return true;
        }

        return false;
    }

    /**
     * <p>
     * untapTargetList.
     * </p>
     * 
     * @param source
     *            a {@link forge.game.card.Card} object.
     * @param tgt
     *            a {@link forge.game.spellability.TargetRestrictions} object.
     * @param af
     *            a {@link forge.game.ability.AbilityFactory} object.
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @param tapList
     *            a {@link forge.CardList} object.
     * @return a boolean.
     */
    private boolean untapTargetList(final Card source, final TargetRestrictions tgt, final SpellAbility sa, final boolean mandatory, final List<Card> tapList) {
        
        for (final Card c : sa.getTargets().getTargetCards()) {
            tapList.remove(c);
        }

        if (tapList.size() == 0) {
            return false;
        }

        while (sa.getTargets().getNumTargeted() < tgt.getMaxTargets(source, sa)) {
            Card choice = null;

            if (tapList.size() == 0) {
                if ((sa.getTargets().getNumTargeted() < tgt.getMinTargets(source, sa)) || (sa.getTargets().getNumTargeted() == 0)) {
                    if (!mandatory) {
                        sa.resetTargets();
                    }
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            if (CardLists.getNotType(tapList, "Creature").size() == 0) {
                choice = ComputerUtilCard.getBestCreatureAI(tapList); // if only
                                                                     // creatures
                                                                     // take
                                                                     // the
                                                                     // best
            } else {
                choice = ComputerUtilCard.getMostExpensivePermanentAI(tapList, sa, false);
            }

            if (choice == null) { // can't find anything left
                if ((sa.getTargets().getNumTargeted() < tgt.getMinTargets(sa.getHostCard(), sa)) || (sa.getTargets().getNumTargeted() == 0)) {
                    if (!mandatory) {
                        sa.resetTargets();
                    }
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            tapList.remove(choice);
            sa.getTargets().add(choice);
        }

        return true;
    }
    
    @Override
    public Card chooseSingleCard(Player ai, SpellAbility sa, Collection<Card> list, boolean isOptional, Player targetedPlayer) {
        return ComputerUtilCard.getBestAI(list);
    }
}
