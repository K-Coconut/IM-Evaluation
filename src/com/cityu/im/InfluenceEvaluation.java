package com.cityu.im;

import com.alibaba.fastjson.JSON;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Ethan
 * @date 2021/6/11 6:10 下午
 */
public class InfluenceEvaluation {
    private final String basePath = "/Users/coconut/code/git/research/GCOMB-Research/IM/IM_TV/GraphSAGE-master/real_data/%s/TV/test/";
    //    private final List<Integer> budgets = new ArrayList<>(Arrays.asList(300, 2725, 5150, 7575, 10000));
    private final List<Integer> budgets = new ArrayList<>(Arrays.asList(10, 20, 50, 100, 200));
    private static Logger log = Logger.getLogger(InfluenceEvaluation.class.getName());

    public static void eval(String dataset, int num) throws IOException {
        InfluenceEvaluation inf = new InfluenceEvaluation();
        log.info(String.format("Loading %s %d RR Sets", dataset, num));
        RRSets rrSets = inf.loadRRSets(dataset, num);
        log.info("RR Sets Loaded");
        ArrayList<Double> influences = inf.evaluation(dataset, 5, rrSets);
        inf.writeResult(dataset, influences);
    }

    public RRSets loadRRSets(String dataset, int num) throws IOException {
        String filePath = String.format(basePath + "/large_graph/mc_RR_Sets/RR%d", dataset, num);
        InputStreamReader in = new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8);
        BufferedReader bufferedReader = new BufferedReader(in);
        String s = bufferedReader.readLine();
        bufferedReader.close();
        return JSON.parseObject(s, RRSets.class);
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
        ArrayList<Double> influences = new ArrayList<>();
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
            log.info(String.format("Budget %d average Influence %f", budget, influence));
            influences.add(influence);
        }
        return influences;
    }

    public void writeResult(String dataset, List<Double> influences) throws IOException {
        String path = String.format(this.basePath + "imm_influence.txt", dataset);
        FileOutputStream outputStream = new FileOutputStream(new File(path));
        OutputStreamWriter ows = new OutputStreamWriter(outputStream);
        for (int i = 0; i < this.budgets.size(); i++) {
            ows.write(String.format("%d:%f\n", this.budgets.get(i), influences.get(i)));
        }
        ows.flush();
        ows.close();
    }
}
