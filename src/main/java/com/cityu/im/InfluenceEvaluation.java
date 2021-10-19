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
    private List<Integer> budgets = new ArrayList<>();
    private HashMap<String, ArrayList<String>> budgetsWithEpochs = new HashMap<>();
    private static Logger log = Logger.getLogger(InfluenceEvaluation.class.getName());

    private void setBudgets(String dataset) {
        HashSet<Integer> budgets = new HashSet<>();
        List<String> files = new ArrayList<>(), results = new ArrayList<>();
        Pattern p = null, p2 = null;
        if (this.mode.equals("imm") || this.mode.equals("interp_imm") || this.mode.equals("imm_dense")) {
            File baseDir = null;
            if (this.mode.equals("imm")) {
                baseDir = new File(String.format(this.basePath, dataset) + "multi_iter/");
            } else if (this.mode.equals("interp_imm")){
                baseDir = new File(String.format(this.basePath, dataset) + "interp/multi_iter/");
            } else {
                baseDir = new File(String.format(this.basePath, dataset) + "multi_iter/dense/");
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
        } else if (this.mode.equals("gcomb_epoch")) {
            File baseDir = new File(String.format(this.basePath, dataset));
            files = Arrays.stream(baseDir.listFiles()).map(File::toString).filter(name -> Pattern.matches(".*large_graph_epoch_\\d+-result_RL_\\d+_nbs_0.003", name)).collect(Collectors.toList());
            p = Pattern.compile("large_graph_epoch_(\\d+)-result_RL_(\\d+)_nbs_0.003");
            for (String file : files) {
                Matcher matcher = p.matcher(file);
                if (!matcher.find()) continue;
                String epoch = matcher.group(1);
                String budget = matcher.group(2);
                if (new File(String.format(this.basePath + "large_graph_epoch_%s_reward_RL_budget_%s_nbs_0.003", dataset, epoch, budget)).exists()) {
                    continue;
                }
                this.budgetsWithEpochs.putIfAbsent(budget, new ArrayList<>());
                this.budgetsWithEpochs.get(budget).add(epoch);
            }
            log.info(String.format("%s Parsing on budgets & epochs: %s", dataset, this.budgetsWithEpochs.toString()));
            return;

        } else if (this.mode.equals("gnn_greedy")) {
            File baseDir = new File(String.format(this.basePath + "/gnn_greedy/", dataset));
            files = Arrays.stream(baseDir.listFiles()).map(File::toString).filter(name -> Pattern.matches(".*budget\\d+_epoch\\d+_seeds.txt", name)).collect(Collectors.toList());
            p = Pattern.compile("budget(\\d+)_epoch(\\d+)_seeds.txt");
            for (String file : files) {
                Matcher matcher = p.matcher(file);
                if (!matcher.find()) continue;
                String budget = matcher.group(1);
                String epoch = matcher.group(2);
                if (new File(String.format(this.basePath  + "gnn_greedy/budget%s_epoch%s_reward.txt", dataset, budget, epoch)).exists()) {
                    continue;
                }
                this.budgetsWithEpochs.putIfAbsent(budget, new ArrayList<>());
                this.budgetsWithEpochs.get(budget).add(epoch);
            }
            log.info(String.format("%s Parsing on budgets & epochs: %s", dataset, this.budgetsWithEpochs.toString()));
            return;
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
        inf.mode = mode;
        inf.basePath = basePath;
        inf.setBudgets(dataset);
        if (inf.budgets.size() == 0 && inf.budgetsWithEpochs.size() == 0) {
            log.info("Nothing to process");
            return;
        }

        GenerateRRSets genRR = new GenerateRRSets();
        genRR.basePath = basePath;
        genRR.readData(dataset);
        log.info(String.format("Graph loaded, size: %.2f G", (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024. / 1024.));
        RRSets rrSets = null;
        boolean gen = !dataset.equals("youtube");
        if (gen) {
            log.info(String.format("Generating %s %d RR Sets", dataset, num));
            Long st = System.currentTimeMillis();
            rrSets = genRR.generateRRSets(num);
            Long ed = System.currentTimeMillis();
            log.info(String.format("%d RR Sets generated, takes %fs", num, (ed - st) / 1000.));
            log.info("RR Sets Loaded, eval on budges " + Arrays.toString(inf.budgets.toArray()));
        } else {
            log.info(String.format("Loading %s %d RR Sets", dataset, num));
            Long st = System.currentTimeMillis();
            rrSets = inf.loadRRSets(dataset, num);
            Long ed = System.currentTimeMillis();
            log.info(String.format("%d RR Sets loaded, takes %fs", num, (ed - st) / 1000.));
        }
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
            ArrayList<Integer> rr = (ArrayList<Integer>) Arrays.asList(text.substring(1, text.length() - 1).split(", ")).stream().map(Integer::parseInt).collect(Collectors.toList());
            rr.trimToSize();
            S.hyperGT.add(rr);
        }
        while ((text = bufferedReader.readLine()) != null) {
            String[] split = text.split(":");
            ArrayList<Integer> rr = (ArrayList<Integer>) Arrays.asList(split[1].substring(1, split[1].length() - 1).split(", ")).stream().map(Integer::parseInt).collect(Collectors.toList());
            rr.trimToSize();
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
        ArrayList<Double> coverageList = new ArrayList<>();

        // special case
        if (this.mode.equals("gcomb_epoch") || this.mode.equals("gnn_greedy")) {
            for (Map.Entry<String, ArrayList<String>> entry : this.budgetsWithEpochs.entrySet()) {
                String budget = entry.getKey();
                for (String epoch : entry.getValue()) {
                    String path, resultPath;
                    if (this.mode.equals("gcomb_epoch")) {
                        path = String.format(this.basePath + "large_graph_epoch_%s-result_RL_%s_nbs_0.003", dataset, epoch, budget);
                        resultPath = String.format(this.basePath + "large_graph_epoch_%s_reward_RL_budget_%s_nbs_0.003", dataset, epoch, budget);
                    } else {
                        path = String.format(this.basePath  + "gnn_greedy/budget%s_epoch%s_seeds.txt", dataset, budget, epoch);
                        resultPath = String.format(this.basePath  + "gnn_greedy/budget%s_epoch%s_reward.txt", dataset, budget, epoch);
                    }
                    double influence = evaluate(S, path);
                    FileOutputStream out = new FileOutputStream(resultPath);
                    OutputStreamWriter ows = new OutputStreamWriter(out);
                    double coverage = influence / S.hyperGT.size();
                    ows.write(String.format("%f", coverage));
                    ows.flush();
                    ows.close();
                    log.info(String.format("Processing budget %s epoch %s, coverage: %f", budget, epoch, coverage));
                }
            }
        }

        for (int budget : this.budgets) {
            String path;
            double influence = 0.;
            if (this.mode.equals("imm") || this.mode.equals("interp_imm") || this.mode.equals("imm_dense")) {
                for (int iter = 0; iter < n_iter; iter++) {
                    if (this.mode.equals("imm")) {
                        path = String.format(this.basePath + "multi_iter/large_graph_ic_imm_sol_eps0.5_num_k_%d_iter_%d.txt", dataset, budget, iter);
                    } else if (this.mode.equals("inteerp_imm")){
                        path = String.format(this.basePath + "interp/multi_iter/large_graph_ic_imm_sol_eps0.5_num_k_%d_iter_%d.txt", dataset, budget, iter);
                    } else {
                        path = String.format(this.basePath + "/multi_iter/dense/large_graph_ic_imm_sol_eps0.5_num_k_%d_iter_%d.txt", dataset, budget, iter);
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
            coverageList.add(influence / S.hyperGT.size());
        }

        return coverageList;
    }

    public double evaluate(RRSets S, String path) throws IOException {
        InputStreamReader in = new InputStreamReader(new FileInputStream(path));
        BufferedReader bufferedReader = new BufferedReader(in);
        List<Integer> seeds = new ArrayList<>();
        if (this.mode.equals("gcomb") || this.mode.equals("gcomb_epoch") || this.mode.equals("gnn_greedy") ||
                this.mode.equals("imm") || this.mode.equals("interp_imm") || this.mode.equals("imm_dense")) {
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
            } else if (this.mode.equals("imm_dense")) {
                path = String.format(this.basePath + "/multi_iter/dense/imm_influence_%d.txt", dataset, this.budgets.get(i));
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
