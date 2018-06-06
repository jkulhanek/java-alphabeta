package cz.cvut.student;

import cz.cvut.fel.aic.zui.gobblet.Gobblet;
import cz.cvut.fel.aic.zui.gobblet.algorithm.Algorithm;
import cz.cvut.fel.aic.zui.gobblet.environment.Board;
import cz.cvut.fel.aic.zui.gobblet.environment.Move;

import java.util.*;

public class AlphaBeta extends Algorithm {
    private final int windowSize;
    private static final int MAX_PLAYER = 0;

    public AlphaBeta(){
        this.windowSize = 1;
    }

    private static int negate(int value){
        // I hate java for the silent overflow sooooo much
        if(value == Integer.MIN_VALUE){
            return Integer.MAX_VALUE;
        }
        else if(value == Integer.MAX_VALUE){
            return Integer.MIN_VALUE;
        }
        else return -value;
    }

    private int callRun(Board board, int depth, int player, int alpha, int beta, boolean negate){
        if(negate) {
            return negate(run(board, depth - 1, Gobblet.switchPlayer(player),
                    negate(beta),
                    negate(alpha)));
        }
        else{
            return run(board, depth - 1, player,
                    alpha,
                    beta);
        }
    }

    @Override
    protected int runImplementation(Board game, int depth, int player, int alpha, int beta) {
        Integer cachedValue = this.tryGetCache(game, depth, player, alpha, beta);
        if(cachedValue!=null){
            return cachedValue;
        }

        int terminateState = game.isTerminate(player);
        if (depth == 0 || terminateState != Board.DUMMY) {
            int coef = player == Board.WHITE_PLAYER ? 1 : -1;
            return coef * game.evaluateBoard();
        }

        List<Move> moves = game.generatePossibleMoves(player);
        moves = orderMoves(moves, depth, game, player);
        int c = 0;
        int b = beta;
        int bestValue = Integer.MIN_VALUE;
        for (Move move : moves) {
            Board subGame = new Board(game);
            subGame.makeMove(move);
            int score;

            if (c > 0) b = alpha + 1;
            score = callRun(subGame, depth, player, alpha, b, true);

            if (c > 0 && score >= b && score < beta && depth > 1) {
                subGame = new Board(game);
                subGame.makeMove(move);
                score = callRun(subGame, depth, player, alpha, beta, true);
            }

            c++;

            bestValue = Math.max(bestValue, score);
            alpha = Math.max(alpha, score);
            if(alpha >= beta){
                break;
            }
        }

        if(bestValue >= alpha){
            // does not fail low
            this.cacheResult(game, depth, bestValue, player, alpha, beta);
        }

        return bestValue;
    }

    private class CacheEntry{
        public CacheEntry(int value, int beta, int alpha){
            this.value = value;
            this.beta = beta;
            this.alpha = alpha;
        }

        int value;
        int beta;
        int alpha;
    }

    private class CacheEntryKey{
        private final int depth;
        private final long key;
        private final int player;

        public CacheEntryKey(Board map, int depth, int player){
            this.key = map.calculateSimpleHash();
            this.depth = depth;
            this.player = player;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.depth, this.key, this.player);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            final CacheEntryKey other = (CacheEntryKey) obj;
            if (!Objects.equals(this.depth, other.depth)) return false;
            if (!Objects.equals(this.player, other.player)) return false;
            if (!Objects.equals(this.key, other.key)) return false;
            return true;
        }
    }

    Map<CacheEntryKey, CacheEntry> stateCache = new HashMap<>();
    private CacheEntryKey getCacheKey(Board board, int depth, int player){
        return new CacheEntryKey(board, depth, player);
    }

    private void cacheResult(Board board, int depth, int value, int player, int alpha, int beta){
        CacheEntryKey hash = getCacheKey(board, depth, player);
        stateCache.put(hash, new CacheEntry(value, beta, alpha));
    }

    private Integer tryGetCache(Board board, int depth, int player, int alpha, int beta){
        CacheEntryKey key = getCacheKey(board, depth, player);
        CacheEntry entry = stateCache.get(key);
        if(entry!=null){
            if(entry.beta >= beta){
                return entry.value;
            }
        }
        return null;
    }

    private List<Move> orderMoves(List<Move> moves, int depth, Board map, int playerToMove){
        moves = simpleHeuristic(moves, map, playerToMove);
        return moves;
    }

    private List<Move> simpleHeuristic(List<Move> moves, Board map, int playerToMove) {
        class heuristicMove implements Comparable<heuristicMove> {
            public heuristicMove(Move move, Integer priority) {
                this.move = move;
                this.priority = priority;
            }

            Move move;
            Integer priority;

            @Override
            public int compareTo(heuristicMove o) {
                return -this.priority.compareTo(o.priority);
            }
        }

        PriorityQueue<heuristicMove> queue = new PriorityQueue<>();

        for (Move move : moves) {
            Board newBoard = new Board(map);
            if (!newBoard.makeMove(move)) {
                System.err.println("Something is terribly wrong in AB!");
            }

            int score = calculateBoardScore(newBoard, Gobblet.switchPlayer(playerToMove));
            queue.add(new heuristicMove(move, score));
        }

        moves.clear();
        while (!queue.isEmpty()) {
            moves.add(queue.poll().move);
        }

        return moves;
    }

    private int calculateBoardScore(Board game, Integer playerToMove){
        int coef = playerToMove == Board.WHITE_PLAYER? -1 : 1;
        return coef * game.evaluateBoard();
    }
}
