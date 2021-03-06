package forge.game.ability.effects;


import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

public class CleanUpEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        Card source = sa.getHostCard();
        final Game game = source.getGame();

        if (sa.hasParam("ClearRemembered")) {
            source.clearRemembered();
            game.getCardState(source).clearRemembered();
        }
        if (sa.hasParam("ForgetDefined")) {
            for (final Card card : AbilityUtils.getDefinedCards(source, sa.getParam("ForgetDefined"), sa)) {
                source.getRemembered().remove(card);
            }
        }
        if (sa.hasParam("ClearImprinted")) {
            source.clearImprinted();
        }
        if (sa.hasParam("ClearChosenX")) {
            source.setSVar("ChosenX", "");
        }
        if (sa.hasParam("ClearTriggered")) {
            game.getTriggerHandler().clearDelayedTrigger(source);
        }
        if (sa.hasParam("ClearCoinFlips")) {
            source.clearFlipResult();
        }
        if (sa.hasParam("ClearChosenCard")) {
            source.getChosenCard().clear();
        }
    }

}
