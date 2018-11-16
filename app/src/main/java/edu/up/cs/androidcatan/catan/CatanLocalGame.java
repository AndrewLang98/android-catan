package edu.up.cs.androidcatan.catan;


import android.util.Log;

import edu.up.cs.androidcatan.catan.actions.CatanBuildCityAction;
import edu.up.cs.androidcatan.catan.actions.CatanBuildRoadAction;
import edu.up.cs.androidcatan.catan.actions.CatanBuildSettlementAction;
import edu.up.cs.androidcatan.catan.actions.CatanBuyDevCardAction;
import edu.up.cs.androidcatan.catan.actions.CatanEndTurnAction;
import edu.up.cs.androidcatan.catan.actions.CatanRobberDiscardAction;
import edu.up.cs.androidcatan.catan.actions.CatanRobberMoveAction;
import edu.up.cs.androidcatan.catan.actions.CatanRobberStealAction;
import edu.up.cs.androidcatan.catan.actions.CatanRollDiceAction;
import edu.up.cs.androidcatan.catan.actions.CatanTradeWithBankAction;
import edu.up.cs.androidcatan.catan.actions.CatanTradeWithCustomPortAction;
import edu.up.cs.androidcatan.catan.actions.CatanTradeWithPortAction;
import edu.up.cs.androidcatan.catan.actions.CatanUseKnightCardAction;
import edu.up.cs.androidcatan.catan.actions.CatanUseMonopolyCardAction;
import edu.up.cs.androidcatan.catan.actions.CatanUseRoadBuildingCardAction;
import edu.up.cs.androidcatan.catan.actions.CatanUseVictoryPointCardAction;
import edu.up.cs.androidcatan.catan.actions.CatanUseYearOfPlentyCardAction;
import edu.up.cs.androidcatan.catan.gamestate.DevelopmentCard;
import edu.up.cs.androidcatan.catan.gamestate.buildings.City;
import edu.up.cs.androidcatan.catan.gamestate.buildings.Road;
import edu.up.cs.androidcatan.catan.gamestate.buildings.Settlement;
import edu.up.cs.androidcatan.game.GamePlayer;
import edu.up.cs.androidcatan.game.LocalGame;
import edu.up.cs.androidcatan.game.actionMsg.GameAction;

/**
 * @author Alex Weininger
 * @author Andrew Lang
 * @author Daniel Borg
 * @author Niraj Mali
 * @version November 8th, 2018
 * https://github.com/alexweininger/android-catan
 **/

public class CatanLocalGame extends LocalGame {

    private final static String TAG = "CatanLocalGame";

    private CatanGameState state;

    CatanLocalGame () {
        super();
        state = new CatanGameState();
    }

    /*--------------------------------------- Action Methods -------------------------------------------*/

    /**
     * Tell whether the given player is allowed to make a move at the
     * present point in the game.
     *
     * @param playerIdx the player's player-number (ID)
     * @return true iff the player is allowed to move
     */
    @Override
    protected boolean canMove (int playerIdx) {
        Log.d(TAG, "canMove() called with: playerIdx = [" + playerIdx + "]");

        if (state.isRobberPhase()) return true; // todo fix this iffy logic

        if (playerIdx < 0 || playerIdx > 3) Log.e(TAG, "canMove: Invalid playerIds: " + playerIdx);

        Log.d(TAG, "canMove() returned: " + (playerIdx == state.getCurrentPlayerId()));
        return playerIdx == state.getCurrentPlayerId();
    }

    /**
     * Initiates action based on what kind of GameAction object received.
     *
     * @param action The move that the player has sent to the game
     * @return Tells whether the move was a legal one.
     */
    @Override
    protected boolean makeMove (GameAction action) {
        Log.d(TAG, "makeMove() called with: action = [" + action + "]");

        /* --------------------------- Turn Actions --------------------------------------- */

        if (action instanceof CatanRollDiceAction) {
            Log.d(TAG, "makeMove() called with: action = [" + action + "]");
            state.setCurrentDiceSum(state.getDice().roll());
            Log.i(TAG, "rollDice: Player " + state.getCurrentPlayerId() + " rolled a " + state.getCurrentDiceSum());

            if (state.getCurrentDiceSum() == 7) { // if the robber is rolled
                Log.i(TAG, "rollDice: The robber has been activated.");
                //            this.isRobberPhase = true;
            } else {
                state.produceResources(state.getCurrentDiceSum());
            }
            state.setActionPhase(true);
            return true;
        }

        if (action instanceof CatanEndTurnAction) {
            Log.d(TAG, "makeMove() called with: action = [" + action + "]");

            state.updateVictoryPoints();
            state.getBoard().getPlayerWithLongestRoad(state.getPlayerList());
            state.setSetupPhase(state.updateSetupPhase());

            // increment the current turn
            if (state.getCurrentPlayerId() == 3) state.setCurrentPlayerId(0);
            else state.setCurrentPlayerId(state.getCurrentPlayerId() + 1);

            state.setActionPhase(false);

            Log.i(TAG, "makeMove: Player " + state.getCurrentPlayerId() + " has ended their turn.");
            return true;
        }

        /* --------------------------- Build Actions --------------------------------------- */

        if (action instanceof CatanBuildRoadAction) {
            Log.d(TAG, "makeMove() receiving a CatanBuildRoadAction: " + action.toString());

            // if it is the setup phase, do not remove resources
            if (((CatanBuildRoadAction) action).isSetupPhase()) {
                Log.i(TAG, "makeMove: Setup phase. Not checking for resources.");
                // add the road to the board
                state.getBoard().addRoad(((CatanBuildRoadAction) action).getOwnerId(), ((CatanBuildRoadAction) action).getIntAId(), ((CatanBuildRoadAction) action).getIntBid());
                return true;
            }
            // if not setup phase, remove the resource cost of a road from the players resource cards
            if (state.getCurrentPlayer().removeResourceBundle(Road.resourceCost)) {
                // add the road to the board
                state.getBoard().addRoad(((CatanBuildRoadAction) action).getOwnerId(), ((CatanBuildRoadAction) action).getIntAId(), ((CatanBuildRoadAction) action).getIntBid());
                return true;
            }
            Log.e(TAG, "makeMove: Player sent a CatanBuildRoadAction but removeResourceBundle returned false.");
            return false;
        }

        if (action instanceof CatanBuildSettlementAction) {
            Log.i(TAG, "makeMove: received an CatanBuildSettlementAction.");

            // if it is the setup phase do not remove resources
            if (((CatanBuildSettlementAction) action).isSetupPhase()) {
                Log.i(TAG, "makeMove: Setup phase. Not checking for resources.");
                // add settlement to the board
                state.getBoard().addBuilding(((CatanBuildSettlementAction) action).getIntersectionId(), new Settlement(((CatanBuildSettlementAction) action).getOwnerId()));
                Log.d(TAG, "makeMove() returned: " + true);
                return true;
            }

            // remove resources from players inventory (also does checks)
            if (state.getCurrentPlayer().removeResourceBundle(Settlement.resourceCost)) {
                // add settlement to the board
                state.getBoard().addBuilding(((CatanBuildSettlementAction) action).getIntersectionId(), new Settlement(((CatanBuildSettlementAction) action).getOwnerId()));
                Log.d(TAG, "makeMove() returned: " + true);
                return true;
            }
            // if the player does not have enough resources at this point in execution something is WRONG
            Log.e(TAG, "buildSettlement: Player " + state.getCurrentPlayerId() + " resources: " + state.getCurrentPlayer().printResourceCards() + " makeMove() returned: " + false);
            return false;
        }

        if (action instanceof CatanBuildCityAction) {
            Log.d(TAG, "makeMove() called with: action = [" + action + "]");

            // remove resources from players inventory (also does checks)
            if (state.getCurrentPlayer().removeResourceBundle(City.resourceCost)) {
                // add building to the board
                state.getBoard().addBuilding(((CatanBuildCityAction) action).getIntersectionId(), new City(((CatanBuildCityAction) action).getOwnerId()));
                Log.d(TAG, "makeMove() returned: " + true);
                return true;
            }
            // if the player does not have enough resources at this point in execution something is WRONG
            Log.e(TAG, "buildCity: Player " + state.getCurrentPlayerId() + " resources: " + state.getCurrentPlayer().printResourceCards() + " makeMove() returned: " + false);
            return false;
        }

        /*------------------------------- Development Card Actions -------------------------------*/

        if (action instanceof CatanBuyDevCardAction) {
            Log.d(TAG, "makeMove() called with: action = [" + action + "]");

            Player player = state.getCurrentPlayer();

            // remove resources from players inventory (also does checks)
            if (!player.removeResourceBundle(DevelopmentCard.resourceCost)) return false;

            // add random dev card to players inventory
            player.getDevelopmentCards().add(state.getRandomCard());
            return true;
        }

        if (action instanceof CatanUseKnightCardAction) {
            Log.d(TAG, "makeMove() called with: action = [" + action + "]");
            state.getCurrentPlayer().removeDevCard(0);
            return true;
        }

        if (action instanceof CatanUseVictoryPointCardAction) {
            Log.d(TAG, "makeMove() called with: action = [" + action + "]");
            state.getCurrentPlayer().removeDevCard(1);
            state.getPlayerList().get(state.getCurrentPlayerId()).addVictoryPointsDevCard();
            return true;
        }

        if (action instanceof CatanUseYearOfPlentyCardAction) {
            Log.d(TAG, "makeMove() called with: action = [" + action + "]");
            state.getCurrentPlayer().addResourceCard(((CatanUseYearOfPlentyCardAction) action).getChosenResource(), 2);
            state.getCurrentPlayer().removeDevCard(2);
            return true;
        }

        if (action instanceof CatanUseMonopolyCardAction) {
            Log.d(TAG, "makeMove() called with: action = [" + action + "]");
            int totalResources = 0;
            int resourceId = ((CatanUseMonopolyCardAction) action).getChosenResource();
            for (Player player : state.getPlayerList()) {
                int resCount = player.getResourceCards()[resourceId];
                player.removeResourceCard(resourceId, resCount);
                totalResources += resCount;
            }
            state.getCurrentPlayer().addResourceCard(resourceId, totalResources);
            state.getCurrentPlayer().removeDevCard(3);
            return true;
        }

        if (action instanceof CatanUseRoadBuildingCardAction) {
            Log.d(TAG, "makeMove() called with: action = [" + action + "]");
            state.getCurrentPlayer().addResourceCard(0, 2);
            state.getCurrentPlayer().addResourceCard(2, 2);
            state.getCurrentPlayer().removeDevCard(4);
            return true;
        }

        /*---------------------------------- Robber Actions --------------------------------------*/

        if (action instanceof CatanRobberDiscardAction) {
            Log.d(TAG, "makeMove() called with: action = [" + action + "]");
            return state.discardResources(((CatanRobberDiscardAction) action).getPlayerId(), ((CatanRobberDiscardAction) action).getRobberDiscardedResources());
        }
        if (action instanceof CatanRobberMoveAction) {
            Log.d(TAG, "makeMove() called with: action = [" + action + "]");

            if (state.getBoard().moveRobber(((CatanRobberMoveAction) action).getHexagonId())) {
                Log.e(TAG, "makeMove() move robber: Player " + ((CatanRobberMoveAction) action).getPlayerId() + " moved the Robber to Hexagon " + ((CatanRobberMoveAction) action).getHexagonId());
                state.setHasMovedRobber(true);
                return true;
            }
            Log.e(TAG, "makeMove: moving the robber failed returning false.");
            return false;
        }
        if (action instanceof CatanRobberStealAction) {
            Log.d(TAG, "makeMove() called with: action = [" + action + "]");
            return state.robberSteal(((CatanRobberStealAction) action).getPlayerId(), ((CatanRobberStealAction) action).getStealId());
        }

        /*---------------------------------- Trade Actions ---------------------------------------*/

        if (action instanceof CatanTradeWithBankAction) {
            Log.d(TAG, "makeMove() called with: action = [" + action + "]");
            // Player.removeResources returns false if the player does not have enough, if they do it removes them.
            if (!state.getCurrentPlayer().removeResourceCard(((CatanTradeWithBankAction) action).getResourceIdGiving(), 4)) {
                Log.e(TAG, "makeMove: trade with bank action: not enough resources, player id: " + state.getCurrentPlayerId());
                return false;
            }
            state.getCurrentPlayer().addResourceCard(((CatanTradeWithBankAction) action).getResourceIdRec(), 1); // add resource card to players inventory
            Log.w(TAG, "tradeWithBank - player " + state.getCurrentPlayerId() + " traded " + 4 + " " + ((CatanTradeWithBankAction) action).getResourceIdGiving() + " for a " + ((CatanTradeWithBankAction) action).getResourceIdRec() + " with bank.\n");
            return true;
        }

        if (action instanceof CatanTradeWithPortAction) {
            Log.d(TAG, "makeMove() called with: action = [" + action + "]");
            // remove resources from the player
            if (state.getCurrentPlayer().removeResourceCard(((CatanTradeWithPortAction) action).getPort().getResourceId(), ((CatanTradeWithPortAction) action).getPort().getTradeRatio())) {
                // add requested resource to player
                state.getCurrentPlayer().addResourceCard(((CatanTradeWithPortAction) action).getResourceRecId(), 1);
                return true;
            } else {
                Log.e(TAG, "makeMove: trade with port: Could not remove resources from player. Returning false");
                return false;
            }
        }

        if (action instanceof CatanTradeWithCustomPortAction) {
            // remove resources from players inventory
            if (state.getCurrentPlayer().removeResourceCard(((CatanTradeWithCustomPortAction) action).getResourceGiveId(), 3)) {
                // add requested resource to player
                state.getCurrentPlayer().addResourceCard(((CatanTradeWithCustomPortAction) action).getResourceRecId(), 1);
            } else {
                Log.e(TAG, "makeMove: custom port trade: Could not remove resources from player. Returning false");
                return false;
            }
        }

        // if we reach here, the GameAction object we received is not one that we recognize
        Log.e(TAG, "makeMove: FATAL ERROR: GameAction action was not and instance of an action class that we recognize.");
        return false;
    }

    /*---------------------- Methods for checking the Game State and updating it ------------------------------------*/

    /**
     * Notify the given player that its state has changed. This should involve sending
     * a GameInfo object to the player. If the game is not a perfect-information game
     * this method should remove any information from the game that the player is not
     * allowed to know.
     *
     * @param p the player to notify
     */
    @Override
    protected void sendUpdatedStateTo (GamePlayer p) {
        Log.d(TAG, "sendUpdatedStateTo() called with: p = [" + p + "]");
        p.sendInfo(new CatanGameState(this.state));
    }

    /**
     * Check if the game is over. It is over, return a string that tells
     * who the winner(s), if any, are. If the game is not over, return null;
     *
     * @return a message that tells who has won the game, or null if the
     * game is not over
     */
    @Override
    protected String checkIfGameOver () {
        Log.d(TAG, "checkIfGameOver() called");
        for (int i = 0; i < this.state.getPlayerVictoryPoints().length; i++) {
            if (this.state.getPlayerVictoryPoints()[i] > 9) {
                return playerNames[i] + " wins!";
            }
        }
        return null; // return null if no winner, but the game is not over
    }
}
