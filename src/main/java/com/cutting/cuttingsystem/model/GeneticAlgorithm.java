package com.cutting.cuttingsystem.model;

import com.cutting.cuttingsystem.entitys.algorithm.Instance;
import com.cutting.cuttingsystem.entitys.algorithm.Solution;
import com.cutting.cuttingsystem.entitys.algorithm.Square;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class GeneticAlgorithm implements CuttingAlgorithm {

    private static final int POP_SIZE = 50;
    private static final int GENERATIONS = 200;
    private static final double MUTATION_RATE = 0.15;
    private static final int TOURNAMENT_SIZE = 5;

    private final Instance instance;
    private final Random random;
    private final TabuSearch evaluator;

    public GeneticAlgorithm(Instance instance) {
        this.instance = instance;
        this.random = new Random();
        try {
            this.evaluator = new TabuSearch(instance);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String name() { return "genetic_algorithm"; }

    @Override
    public String displayName() { return "遗传算法"; }

    @Override
    public Solution search() {
        if (instance.getSquareList() == null || instance.getSquareList().isEmpty()) {
            Solution empty = new Solution();
            empty.setInstance(instance);
            empty.setSquareList(List.of());
            empty.setPlaceSquareList(List.of());
            empty.setRate(0);
            return empty;
        }

        List<List<Square>> population = initPopulation();
        Map<String, Solution> cache = new HashMap<>();

        Solution best = evaluatePermutation(population.get(0), cache);
        int bestGen = 0;

        for (int gen = 0; gen < GENERATIONS; gen++) {
            List<List<Square>> newPop = new ArrayList<>();

            // elitism: keep best 2
            population.sort(Comparator.comparingDouble(p -> -evalRate(p, cache)));
            newPop.add(new ArrayList<>(population.get(0)));
            newPop.add(new ArrayList<>(population.get(1)));

            while (newPop.size() < POP_SIZE) {
                List<Square> parent1 = tournamentSelect(population, cache);
                List<Square> parent2 = tournamentSelect(population, cache);
                List<Square> child = orderCrossover(parent1, parent2);
                if (random.nextDouble() < MUTATION_RATE) {
                    swapMutate(child);
                }
                newPop.add(child);
            }

            population = newPop;
            Solution genBest = evaluatePermutation(population.get(0), cache);
            if (genBest.getRate() > best.getRate()) {
                best = genBest;
                bestGen = gen;
            }
        }

        log.info("GA best generation: {}, rate: {}", bestGen, best.getRate());
        return best;
    }

    private List<List<Square>> initPopulation() {
        List<Square> base = new ArrayList<>(instance.getSquareList());
        List<List<Square>> pop = new ArrayList<>();
        for (int i = 0; i < POP_SIZE; i++) {
            List<Square> perm = new ArrayList<>(base);
            Collections.shuffle(perm, random);
            pop.add(perm);
        }
        return pop;
    }

    private double evalRate(List<Square> perm, Map<String, Solution> cache) {
        return evaluatePermutation(perm, cache).getRate();
    }

    private Solution evaluatePermutation(List<Square> perm, Map<String, Solution> cache) {
        String key = permKey(perm);
        Solution cached = cache.get(key);
        if (cached != null) return cached;
        Solution s = evaluator.evaluate(perm);
        cache.put(key, s);
        return s;
    }

    private String permKey(List<Square> perm) {
        StringBuilder sb = new StringBuilder();
        for (Square s : perm) sb.append(s.getId()).append(',');
        return sb.toString();
    }

    private List<Square> tournamentSelect(List<List<Square>> pop, Map<String, Solution> cache) {
        List<Square> best = null;
        double bestRate = -1;
        for (int i = 0; i < TOURNAMENT_SIZE; i++) {
            List<Square> candidate = pop.get(random.nextInt(pop.size()));
            double rate = evalRate(candidate, cache);
            if (rate > bestRate) {
                bestRate = rate;
                best = candidate;
            }
        }
        return new ArrayList<>(best);
    }

    /** Order crossover (OX) */
    private List<Square> orderCrossover(List<Square> p1, List<Square> p2) {
        int n = p1.size();
        List<Square> child = new ArrayList<>(Collections.nCopies(n, null));
        int cut1 = random.nextInt(n);
        int cut2 = random.nextInt(n);
        if (cut1 > cut2) { int tmp = cut1; cut1 = cut2; cut2 = tmp; }

        Set<String> used = new HashSet<>();
        for (int i = cut1; i <= cut2; i++) {
            child.set(i, p1.get(i));
            used.add(p1.get(i).getId());
        }

        int idx = (cut2 + 1) % n;
        for (int i = 0; i < n; i++) {
            Square candidate = p2.get((cut2 + 1 + i) % n);
            if (!used.contains(candidate.getId())) {
                child.set(idx, candidate);
                idx = (idx + 1) % n;
            }
        }
        return child;
    }

    private void swapMutate(List<Square> perm) {
        int i = random.nextInt(perm.size());
        int j = random.nextInt(perm.size());
        Collections.swap(perm, i, j);
    }
}
