package forge.ai.ability;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import forge.ai.ComputerUtil;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardPredicates;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.List;

/** 
 * AbilityFactory for Creature Spells.
 *
 */
public class PermanentCreatureAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        String logic = sa.getParam("AILogic");
        Game game = aiPlayer.getGame();

        if ("ZeroToughness".equals(logic)) {
            // If Creature has Zero Toughness, make sure some static ability is in play
            // That will grant a toughness bonus

            final List<Card> list = aiPlayer.getCardsIn(ZoneType.Battlefield);
            if (!Iterables.any(list, Predicates.or(CardPredicates.nameEquals("Glorious Anthem"),
                    CardPredicates.nameEquals("Gaea's Anthem")))) {
                return false;
            }

            // TODO See if card ETB will survive after Static Effects
            /*
            List<Card> cards = game.getCardsIn(ZoneType.Battlefield);

            for(Card c : cards) {
                ArrayList<StaticAbility> statics = c.getStaticAbilities();
                for(StaticAbility s : statics) {
                    final Map<String, String> stabMap = s.parseParams();

                    if (!stabMap.get("Mode").equals("Continuous")) {
                        continue;
                    }

                    final String affected = stabMap.get("Affected");

                    if (affected == null) {
                        continue;
                    }
                }
            }
            */
        }

        // Wait for Main2 if possible
        if (game.getPhaseHandler().is(PhaseType.MAIN1)
                && !ComputerUtil.castPermanentInMain1(aiPlayer, sa)) {
            return false;
        }

        // AI shouldn't be retricted all that much for Creatures for now
        return true;
    }

}
