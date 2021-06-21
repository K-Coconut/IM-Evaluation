package com.cityu.im;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.regex.*;

/**
 * @author Ethan
 * @date 2021/6/11 6:10 下午
 */
public class InfluenceEvaluation {
    private String basePath = "";
    private String mode;
    private List<Integer> budgets;
    private static Logger log = Logger.getLogger(InfluenceEvaluation.class.getName());

    private void setBudgets(String dataset) {
        File baseDir = new File(String.format(this.basePath, dataset) + "multi_iter/");
        List<String> files = Arrays.stream(baseDir.listFiles()).map(File::toString).filter(name -> Pattern.matches(".*large_graph_ic_imm_sol_eps0.5_num_k_\\d+_iter_0.txt", name)).collect(Collectors.toList());
        List<String> results = Arrays.stream(baseDir.listFiles()).map(File::toString).filter(name -> Pattern.matches(".*imm_influence_\\d+.txt", name)).collect(Collectors.toList());
        Pattern p = Pattern.compile("large_graph_ic_imm_sol_eps0.5_num_k_(\\d+)_iter_0\\.txt");
        Pattern p2 = Pattern.compile("imm_influence_(\\d+).txt");
        HashSet<Integer> budgets = new HashSet<>();
        for (String name : files) {
            Matcher m = p.matcher(name);
            if (m.find()) {
                int budget = Integer.parseInt(m.group(1));
                budgets.add(budget);
            }
        }
        for (String name : results) {
            Matcher m = p2.matcher(name);
            if (m.find()) {
                int budget = Integer.parseInt(m.group(1));
                budgets.remove(budget);
            }
        }

        this.budgets = new ArrayList<>(budgets);

    }

    public static void eval(String mode, String dataset, int num, int nIter, String basePath) throws IOException {
        InfluenceEvaluation inf = new InfluenceEvaluation();
        inf.mode = mode;
        inf.basePath = basePath;
        inf.setBudgets(dataset);
        log.info(String.format("Loading %s %d RR Sets", dataset, num));
        RRSets rrSets = inf.loadRRSets(dataset, num);
        log.info("RR Sets Loaded, eval on budges " + Arrays.toString(inf.budgets.toArray()));
        ArrayList<Double> coverage = inf.evaluate(dataset, nIter, rrSets);
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

    public double evaluate(RRSets S, List<Integer> seeds) {
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

    public ArrayList<Double> evaluate(String dataset, int n_iter, RRSets S) throws IOException {
        ArrayList<Double> coverage = new ArrayList<>();
        for (int budget : this.budgets) {
            String path;
            double influence = 0.;
            if (this.mode.equals("imm")) {
                for (int iter = 0; iter < n_iter; iter++) {
                    path = String.format(this.basePath + "multi_iter/large_graph_ic_imm_sol_eps0.5_num_k_%d_iter_%d.txt", dataset, budget, iter);
                    influence += evaluate(S, path);
                }
                influence /= n_iter;
            } else if (this.mode.equals("gcomb")) {
                path = String.format(this.basePath + "large_graph-result_RL_%s_nbs_0.003", dataset, budget);
                influence = evaluate(S, path);
            }
            log.info(String.format("Budget %d average Influence: %f, coverage: %f", budget, influence, influence / S.hyperGT.size()));
            coverage.add(influence / S.hyperGT.size());
        }

        return coverage;
    }

    public double evaluate(RRSets S, String path) throws IOException {
        InputStreamReader in = new InputStreamReader(new FileInputStream(path));
        BufferedReader bufferedReader = new BufferedReader(in);
        List<Integer> seeds = new ArrayList<>();
        String seed;
        while ((seed = bufferedReader.readLine()) != null) {
            seeds.add(Integer.parseInt(seed));
        }
        return evaluate(S, seeds);
    }

    public void writeResult(String dataset, List<Double> coverage) throws IOException {
        if (this.mode.equals("imm")) {
            for (int i = 0; i < this.budgets.size(); i++) {
                String path = "";
                if (this.mode.equals("gcomb")) {
                    path = String.format(this.basePath + "large_graph_reward_RL_budget_%s_nbs_0.003", dataset, this.budgets.get(i));
                } else {
                    path = String.format(this.basePath + "multi_iter/imm_influence_%d.txt", dataset, this.budgets.get(i));
                }
                FileOutputStream outputStream = new FileOutputStream(new File(path));
                OutputStreamWriter ows = new OutputStreamWriter(outputStream);
                ows.write(String.format("%f", coverage.get(i)));
                ows.flush();
                ows.close();
            }

        }


    }
}
