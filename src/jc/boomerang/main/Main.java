package jc.boomerang.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

// Program to calculate the possible outcomes of a boomerang tournament,
// Facebook Hacker Cup 2016, round 1, problem 4. Good illustration of the
// 'dynamic programming' technique.
//
// (https://www.facebook.com/hackercup/problem/1424196571244550/)
//
public class Main {
	// keep some stats
	static long loopCount;
	static long maxLoopCount;
	
	// Class representing the possible winners of a tournament consisting
	// of a subset of the players.
	// - playerBits has bit i set if player i+1 is in this subtournament
	// - winnerBits has bit i set if player i+1 could win this subtournament
	//
	public static class DPRow {
		DPRow(int nPlayers, int playerBits) {
			this.playerBits = playerBits;
			this.winnerBits = 0;
		}
		int playerBits;
		int winnerBits;
	}

	// just for map sizing, although the performance of the Integer hash
	// will be so poor that it probably doesn't matter much :)
	static int binomial(int n, int k) {
		if (k > n / 2)
			k = n - k;
		
	    int ret = 1;
	    for (int i = 0; i < k; i++) {
	        ret = (ret * (n - i)) / (i + 1);
	    }
	    return ret;
	}

	public static void setWinners(int nPlayers, int[] wins,
			DPRow oldRow1, DPRow oldRow2,
			DPRow newRow) {
		// loop through all the players
		for (int i = 0, w = 1; i < nPlayers; i++, w = w << 1) {
			loopCount++;
			
			// check if this player could win the first subtournament
			if ((oldRow1.winnerBits & w) != 0) {
				// yep, so see if player i wins against any of the potential
				// winners of the second subtournament 
				if ((oldRow2.winnerBits & wins[i]) != 0) {
					// yes, so player i could win this subtournament
					newRow.winnerBits |= w;
				}
			}
			
			// otherwise, check if this player could win the second subtournament
			// (no player can be in both subtournaments)
			else if ((oldRow2.winnerBits & w) != 0) {
				// yep, so see if player i wins against any of the potential
				// winners of the first subtournament 
				if ((oldRow1.winnerBits & wins[i]) != 0) {
					// yes, so player i could win this subtournament
					newRow.winnerBits |= w;
				}
			}
		}
	}

	// Given the potential winners from the previous round, in oldRows,
	// calculate and return a new map of rows containing the potential
	// winners after the next round of the tournament.
	//
	public static Map<Integer, DPRow> nextRound(
			Map<Integer, DPRow> oldRows,
			int nPlayers,
			int[] wins,
			int subTournamentSize) {
		
		// create the result map with a decent size, although bucket distribution
		// will be horribly uneven
		Map<Integer, DPRow> newRows = new HashMap<>(binomial(nPlayers, subTournamentSize));
		
		// convenient to have the old rows in an array
		DPRow[] oldRowsArray = new DPRow[oldRows.size()];
		oldRowsArray = oldRows.values().toArray(oldRowsArray);
		
		// loop through all pairs of subtournament results from the
		// previous round
		for (int i = 0; i < oldRowsArray.length - 1; i++) {
			for (int j = i + 1; j < oldRowsArray.length; j++) {
				
				// but throw out results where some player was in both subtournaments
				if ((oldRowsArray[i].playerBits & oldRowsArray[j].playerBits) != 0)
					continue;
				
				// create the new playerBits mask for this row
				int newBits = oldRowsArray[i].playerBits | oldRowsArray[j].playerBits;
				
				// we may already have created this row from some other subtournament
				// combination
				DPRow newRow = newRows.get(newBits);
				if (newRow == null) {
					// if not, then go ahead and create it now
					newRow = new DPRow(nPlayers, newBits);
					newRows.put(newBits, newRow);
				}
				
				// calculate who could win this round given the potential winners
				// from these two previous-round subtournaments 
				setWinners(nPlayers, wins, oldRowsArray[i], oldRowsArray[j], newRow);
			}
		}
		
		return newRows;
	}
	
	// The last round can be significantly optimised:
	// - for each possible 'first subtournament', there is no need to go searching
	//   through the oldRows to find 'second subtournament's that have no players
	//   in common with the first - there's only one and we can calculate its
	//   playerBits key directly.
	// - we know there will only be one row returned, so we can avoid the map lookups.
	//	
	public static Map<Integer, DPRow> lastRound(
			Map<Integer, DPRow> oldRows,
			int nPlayers,
			int[] wins) {
		// create the single output row that will result
		int bits = (1 << nPlayers) - 1;
		DPRow newRow = new DPRow(nPlayers, bits);
		
		// loop through all possible 'first subtournament's
		for (DPRow oldRow1 : oldRows.values()) {
			// avoid checking each pair twice
			if ((oldRow1.playerBits & 1) != 0)
				continue;

			// get the 'second subtournament' that has no players in common
			// with the first one
			DPRow oldRow2 = oldRows.get(bits ^ oldRow1.playerBits);
			
			// calculate who could win this round given the potential winners
			// from these two previous-round subtournaments 
			setWinners(nPlayers, wins, oldRow1, oldRow2, newRow);
		}
		
		Map<Integer, DPRow> newRows = new HashMap<>();
		newRows.put(newRow.playerBits, newRow);
		return newRows;
	}

	// read in the next case, process it, and print the results
	public static void processCase(int iCase, BufferedReader reader) throws NumberFormatException, IOException {
		// read the number of players
		int nPlayers = Integer.parseInt(reader.readLine());
		
		// read the wins matrix
		int[] wins = new int[nPlayers];
		for (int i = 0; i < nPlayers; i++) {
			String[] w = reader.readLine().split(" ");
			for (int j = 0; j < nPlayers; j++) {
				if (Integer.parseInt(w[j]) != 0) {
					wins[i] |= (1 << j); 
				}
			}
		}
		
		// Initialize the maxPlacingMinus1 array - everyone starts with a placing
		// of nPlayers/2 + 1. We actually track the placing minus one, to keep
		// the arithmetic simple.
		int[] maxPlacingMinus1 = new int[nPlayers];
		for (int i = 0; i < nPlayers; i++)
			maxPlacingMinus1[i] = (nPlayers / 2);
		
		// Create the initial DP array for 'round 0' - everyone is still alive! 
		Map<Integer, DPRow> dpRows = new HashMap<>(nPlayers);
		for (int i = 0; i < nPlayers; i++) {
			DPRow row = new DPRow(nPlayers, 1 << i);
			row.winnerBits |= 1 << i;
			dpRows.put(1 << i, row);
		}
		
		// Calculate possible results after each 'round' of the tournament -
		// a 'round' consists of each surviving player playing his/her next match.
		for (int step = 1; step < nPlayers; step = step << 1) {
			if (step == nPlayers / 2) {
				// The last round can be significantly optimised
				dpRows = lastRound(dpRows, nPlayers, wins);
			} else {
				dpRows = nextRound(dpRows, nPlayers, wins, step);
			}
			
			// Everyone whose 'winnerBit' is set in some DPRow could have
			// survived this round.
			boolean[] survivors = new boolean[nPlayers];
			for (DPRow row : dpRows.values()) {
				for (int i = 0, w = 1; i < nPlayers; i++, w = w << 1){ 
					if ((row.winnerBits & w) != 0)
						survivors[i] = true;
				}
			}

			// Surviving a round means the number of players who can place higher
			// than you has been halved.
			for (int i = 0; i < nPlayers; i++) {
				if (survivors[i])
					maxPlacingMinus1[i] = maxPlacingMinus1[i] / 2;
			}
		}
		
		// Find the min placing for each player - if a player beats all the other
		// players, then they are guaranteed to win the tournament, otherwise
		// they could be knocked out in the first round for a placing of
		// nPlayers / 2 + 1.
		int[] minPlacing = new int[nPlayers];
		for (int i = 0; i < nPlayers; i++) {
			minPlacing[i] = 1;
			for (int j = 0; j < nPlayers; j++) {
				if (i != j && ((wins[i] & (1 << j)) == 0)) {
					minPlacing[i] = (nPlayers / 2) + 1;
					break;
				}
			}
		}
		
		// produce the output
		System.out.printf("Case #%d:\n", iCase);
		for (int i = 0; i < nPlayers; i++)
			System.out.printf("%d %d\n",  maxPlacingMinus1[i] + 1, minPlacing[i]);

//		System.err.printf("Case %d, loop count %d\n", iCase, loopCount);
//		if (loopCount > maxLoopCount)
//			maxLoopCount = loopCount;
	}

	public static void main(String[] args) throws IOException {
		long now = System.currentTimeMillis();
		
		// read in the number of cases
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		int nCases = Integer.parseInt(reader.readLine());
		
		// read in each case and process it
		for (int i = 1; i <= nCases; i++) {
			loopCount = 0;
			processCase(i, reader);
		}
		
//		System.err.printf("Total run time %d millis\n",  System.currentTimeMillis() - now);
//		System.err.printf("Max loop count %d\n", maxLoopCount);
	}
}
