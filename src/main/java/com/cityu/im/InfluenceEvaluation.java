package com.cityu.im;

import com.alibaba.fastjson.JSON;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Ethan
 * @date 2021/6/11 6:10 下午
 */
public class InfluenceEvaluation {
    private String basePath = "";
    //    private final List<Integer> budgets = new ArrayList<>(Arrays.asList(300, 2725, 5150, 7575, 10000));
    private List<Integer> budgets;
    private static Logger log = Logger.getLogger(InfluenceEvaluation.class.getName());

    public static void eval(String dataset, int num, String basePath, List<Integer> budgets) throws IOException {
        InfluenceEvaluation inf = new InfluenceEvaluation();
        inf.basePath = basePath;
        inf.budgets = budgets;
        log.info(String.format("Loading %s %d RR Sets", dataset, num));
        RRSets rrSets = inf.loadRRSets(dataset, num);
        log.info("RR Sets Loaded, eval on budges " + Arrays.toString(budgets.toArray()));
        ArrayList<Double> coverage = inf.evaluation(dataset, 5, rrSets);
        inf.writeResult(dataset, coverage);
    }

    public RRSets loadRRSets(String dataset, int num) throws IOException {
        String filePath = String.format(basePath + "/large_graph/mc_RR_Sets/RR%d", dataset, num);
        InputStreamReader in = new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8);
        BufferedReader bufferedReader = new BufferedReader(in);
        RRSets S = new RRSets();
        String text = null;
        while ((text = bufferedReader.readLine()) != null && !text.equals("")) {
            List<Integer> rr = Arrays.asList(text.substring(1, text.length() - 1).split(", ")).stream().map(Integer::parseInt).collect(Collectors.toList());
            S.hyperGT.add(rr);
        }
        while ((text = bufferedReader.readLine()) != null) {
            String[] split = text.split(":");
            List<Integer> rr = Arrays.asList(split[1].substring(1, split[1].length() - 1).split(", ")).stream().map(Integer::parseInt).collect(Collectors.toList());
            S.hyperG.put(Integer.parseInt(split[0]), rr);
        }
        bufferedReader.close();
        return S;
    }

    public double evaluation(RRSets S, List<Integer> seeds) {
        int cover = 0;
        boolean[] visitedRR = new boolean[S.hyperGT.size()];

        for (int seed : seeds) {
            for (int rrId : S.hyperG.getOrDefault(seed, new ArrayList<>())) {
                if (!visitedRR[rrId]) {
                    cover += 1;
                    visitedRR[rrId] = true;
                }
            }
        }
        return cover;
    }

    public ArrayList<Double> evaluation(String dataset, int n_iter, RRSets S) throws IOException {
        ArrayList<Double> coverage = new ArrayList<>();
        for (int budget : budgets) {
            double influence = 0.;
            for (int iter = 0; iter < n_iter; iter++) {
                String path = String.format(this.basePath + "multi_iter/large_graph_ic_imm_sol_eps0.5_num_k_%d_iter_%d.txt", dataset, budget, iter);
                InputStreamReader in = new InputStreamReader(new FileInputStream(path));
                BufferedReader bufferedReader = new BufferedReader(in);
                List<Integer> seeds = new ArrayList<>();
                String seed;
                while ((seed = bufferedReader.readLine()) != null) {
                    seeds.add(Integer.parseInt(seed));
                }
                influence += evaluation(S, seeds);
            }
            influence /= n_iter;
            log.info(String.format("Budget %d average Influence: %f, coverage: %f", budget, influence, influence / S.hyperGT.size()));
            coverage.add(influence / S.hyperGT.size());
        }
        return coverage;
    }

    public void writeResult(String dataset, List<Double> coverage) throws IOException {
        String path = String.format(this.basePath + "imm_influence.txt", dataset);
        FileOutputStream outputStream = new FileOutputStream(new File(path));
        OutputStreamWriter ows = new OutputStreamWriter(outputStream);
        for (int i = 0; i < this.budgets.size(); i++) {
            ows.write(String.format("%d:%f\n", this.budgets.get(i), coverage.get(i)));
        }
        ows.flush();
        ows.close();
    }
}
