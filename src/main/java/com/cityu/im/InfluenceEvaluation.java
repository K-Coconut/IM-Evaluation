package com.cityu.im;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.regex.*;
import java.util.stream.Stream;

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
        HashSet<Integer> budgets = new HashSet<>();
        List<String> files = new ArrayList<>(), results = new ArrayList<>();
        Pattern p = null, p2 = null;
        if (this.mode.equals("imm") || this.mode.equals("interp_imm")) {
            File baseDir = null;
            if (this.mode.equals("imm")) {
                baseDir = new File(String.format(this.basePath, dataset) + "multi_iter/");
            } else {
                baseDir = new File(String.format(this.basePath, dataset) + "interp/multi_iter/");
            }
            files = Arrays.stream(baseDir.listFiles()).map(File::toString).filter(name -> Pattern.matches(".*large_graph_ic_imm_sol_eps0.5_num_k_\\d+_iter_0.txt", name)).collect(Collectors.toList());
            results = Arrays.stream(baseDir.listFiles()).map(File::toString).filter(name -> Pattern.matches(".*imm_influence_\\d+.txt", name)).collect(Collectors.toList());
            p = Pattern.compile("large_graph_ic_imm_sol_eps0.5_num_k_(\\d+)_iter_0\\.txt");
            p2 = Pattern.compile("imm_influence_(\\d+).txt");

        }
        else if (this.mode.equals("gcomb")) {
            File baseDir = new File(String.format(this.basePath, dataset));
            files = Arrays.stream(baseDir.listFiles()).map(File::toString).filter(name -> Pattern.matches(".*large_graph-result_RL_\\d+_nbs_0.003", name)).collect(Collectors.toList());
            results = Arrays.stream(baseDir.listFiles()).map(File::toString).filter(name -> Pattern.matches(".*large_graph_reward_RL_budget_\\d+_nbs_0.003", name)).collect(Collectors.toList());
            p = Pattern.compile("large_graph-result_RL_(\\d+)_nbs_0.003");
            p2 = Pattern.compile("large_graph_reward_RL_budget_(\\d+)_nbs_0.003");
        } else if (this.mode.equals("SingleDiscount") || this.mode.equals("DDiscount")) {
            File baseDir = new File(String.format(this.basePath, dataset));
            files = Arrays.stream(baseDir.listFiles()).map(File::toString).filter(name -> Pattern.matches(String.format(".*%s_budget_\\d.*", this.mode), name)).collect(Collectors.toList());
            results = Arrays.stream(baseDir.listFiles()).map(File::toString).filter(name -> Pattern.matches(String.format(".*%s_reward_\\d.*", this.mode), name)).collect(Collectors.toList());
            p = Pattern.compile(String.format(".*%s_budget_(\\d+).*", this.mode));
            p2 = Pattern.compile(String.format(".*%s_reward_(\\d+).*", this.mode));
        }
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
        this.budgets =  budgets.stream().sorted().collect(Collectors.toList());
        log.info(String.format("%s Parsing on budgets: %s", dataset, this.budgets.toString()));

    }

    public static void eval(String mode, String dataset, int num, int nIter, String basePath) throws IOException {
        InfluenceEvaluation inf = new InfluenceEvaluation();
        GenerateRRSets genRR = new GenerateRRSets();
        genRR.basePath = basePath;
        genRR.readData(dataset);
        inf.mode = mode;
        inf.basePath = basePath;
        inf.setBudgets(dataset);
        if (inf.budgets.size() == 0) {
            return;
        }
        log.info(String.format("Generating %s %d RR Sets", dataset, num));
        Long st = System.currentTimeMillis();
        RRSets rrSets = genRR.generateRRSets(num);
        Long ed = System.currentTimeMillis();
        log.info(String.format("%d RR Sets generated, takes %fs", num, (ed - st) / 1000.));
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
            if (this.mode.equals("imm") || this.mode.equals("interp_imm")) {
                for (int iter = 0; iter < n_iter; iter++) {
                    if (this.mode.equals("imm")) {
                        path = String.format(this.basePath + "multi_iter/large_graph_ic_imm_sol_eps0.5_num_k_%d_iter_%d.txt", dataset, budget, iter);
                    } else {
                        path = String.format(this.basePath + "interp/multi_iter/large_graph_ic_imm_sol_eps0.5_num_k_%d_iter_%d.txt", dataset, budget, iter);
                    }
                    influence += evaluate(S, path);
                }
                influence /= n_iter;
            } else if (this.mode.equals("gcomb")) {
                path = String.format(this.basePath + "large_graph-result_RL_%s_nbs_0.003", dataset, budget);
                influence = evaluate(S, path);
            } else if (this.mode.equals("DDiscount") || this.mode.equals("SingleDiscount")) {
                path = String.format(this.basePath + "%s_budget_%d.txt", dataset, this.mode, budget);
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
        if (this.mode.equals("gcomb") || this.mode.equals("imm") || this.mode.equals("interp_imm")) {
            String seed;
            while ((seed = bufferedReader.readLine()) != null) {
                seeds.add(Integer.parseInt(seed));
            }
        } else {
            String line = bufferedReader.readLine();
            String s = line.split(":")[2];
            seeds = Arrays.stream(s.substring(1, s.length() - 1).split(", ")).map(Integer::parseInt).collect(Collectors.toList());
        }

        return evaluate(S, seeds);
    }

    public void writeResult(String dataset, List<Double> coverage) throws IOException {
        for (int i = 0; i < this.budgets.size(); i++) {
            String path = "";
            if (this.mode.equals("gcomb")) {
                path = String.format(this.basePath + "large_graph_reward_RL_budget_%s_nbs_0.003", dataset, this.budgets.get(i));
            } else if (this.mode.equals("imm")){
                path = String.format(this.basePath + "multi_iter/imm_influence_%d.txt", dataset, this.budgets.get(i));
            } else if (this.mode.equals("interp_imm")) {
                path = String.format(this.basePath + "interp/multi_iter/imm_influence_%d.txt", dataset, this.budgets.get(i));
            }
            else {
                path = String.format(this.basePath + "%s_reward_%d.txt", dataset, this.mode, this.budgets.get(i));
            }
            FileOutputStream outputStream = new FileOutputStream(new File(path));
            OutputStreamWriter ows = new OutputStreamWriter(outputStream);
            ows.write(String.format("%f", coverage.get(i)));
            ows.flush();
            ows.close();
        }

    }
}
