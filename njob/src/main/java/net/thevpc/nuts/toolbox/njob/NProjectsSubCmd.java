package net.thevpc.nuts.toolbox.njob;

import net.thevpc.nuts.*;
import net.thevpc.nuts.cmdline.NArg;
import net.thevpc.nuts.cmdline.NCmdLine;
import net.thevpc.nuts.format.NMutableTableModel;
import net.thevpc.nuts.format.NTableFormat;
import net.thevpc.nuts.io.NPrintStream;
import net.thevpc.nuts.spi.NScopeType;
import net.thevpc.nuts.text.NText;
import net.thevpc.nuts.text.NTextStyle;
import net.thevpc.nuts.text.NTexts;
import net.thevpc.nuts.toolbox.njob.model.NProject;
import net.thevpc.nuts.toolbox.njob.time.TimeParser;
import net.thevpc.nuts.toolbox.njob.time.WeekDay;
import net.thevpc.nuts.util.NMsg;
import net.thevpc.nuts.util.NRef;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NProjectsSubCmd {

    private JobService service;
    private NSession session;
    private JobServiceCmd parent;

    public NProjectsSubCmd(JobServiceCmd parent) {
        this.parent = parent;
        this.session = parent.session;
        this.service = parent.service;
        this.session = parent.session;
    }

    public void runProjectAdd(NCmdLine cmd) {
        NProject t = new NProject();
        NRef<Boolean> list = NRef.of(false);
        NRef<Boolean> show = NRef.of(false);
        while (cmd.hasNext()) {
            NArg aa = cmd.peek().get();
            switch (aa.key()) {
                case "--list":
                case "-l": {
                    cmd.matcher().matchFlag((v) -> list.set(v.booleanValue())).anyMatch();
                    break;
                }
                case "--show":
                case "-s": {
                    cmd.matcher().matchFlag((v) -> show.set(v.booleanValue())).anyMatch();
                    break;
                }
                case "-t":
                case "--start":
                case "--on": {
                    cmd.matcher().matchEntry((v) -> t.setStartTime(new TimeParser().parseInstant(v.stringValue(), false))).anyMatch();
                    break;
                }
                case "--at": {
                    cmd.matcher().matchEntry((v) -> t.setStartTime(new TimeParser().setTimeOnly(true).parseInstant(v.stringValue(), false))).anyMatch();
                    break;
                }
                case "-b":
                case "--beneficiary":
                case "--for": {
                    cmd.matcher().matchEntry((v) -> t.setBeneficiary(v.stringValue())).anyMatch();
                    break;
                }
                case "-c":
                case "--company":
                case "--via": {
                    cmd.matcher().matchEntry((v) -> t.setCompany(v.stringValue())).anyMatch();
                    break;
                }
                case "-1":
                case "--day1": {
                    cmd.matcher().matchEntry((v) -> t.setStartWeekDay(WeekDay.parse(v.stringValue()))).anyMatch();
                    break;
                }
                case "-o":
                case "--obs": {
                    cmd.matcher().matchEntry((v) -> t.setObservations(v.stringValue())).anyMatch();
                    break;
                }
                default: {
                    if (aa.isNonOption()) {
                        if (t.getName() == null) {
                            t.setName(cmd.next().get().toString());
                        } else {
                            cmd.throwUnexpectedArgument();
                        }
                    } else {
                        cmd.throwUnexpectedArgument();
                    }
                }
            }
        }
        if (cmd.isExecMode()) {
            service.projects().addProject(t);
            if (session.isPlainTrace()) {
                NOut.println(NMsg.ofC("project %s (%s) added.",
                        NText.ofStyledPrimary5(t.getId()),
                        t.getName()
                ));
            }
            if (show.get()) {
                runProjectShow(NCmdLine.of(new String[]{t.getId()}));
            }
            if (list.get()) {
                runProjectList(NCmdLine.of(new String[0]));
            }
        }
    }

    public void runProjectUpdate(NCmdLine cmd) {
        class Data {
            List<NProject> projects = new ArrayList<>();
            boolean list = false;
            boolean show = false;
            String mergeTo = null;
            List<Consumer<NProject>> runLater = new ArrayList<>();
        }
        Data d = new Data();
        while (cmd.hasNext()) {
            NArg aa = cmd.peek().get();
            switch (aa.key()) {
                case "-l":
                case "--list": {
                    cmd.matcher().matchFlag((v) -> d.list = v.booleanValue()).anyMatch();
                    break;
                }
                case "-s":
                case "--show": {
                    cmd.matcher().matchFlag((v) -> d.show = v.booleanValue()).anyMatch();
                    break;
                }
                case "--on":
                case "--start": {
                    cmd.matcher().matchEntry((v) -> d.runLater.add(t -> t.setStartTime(new TimeParser().parseInstant(v.stringValue(), false)))).anyMatch();
                    break;
                }
                case "--at": {
                    cmd.matcher().matchEntry((v) -> d.runLater.add(t -> t.setStartTime(new TimeParser().setTimeOnly(true).parseInstant(v.stringValue(), false)))).anyMatch();
                    break;
                }
                case "--for":
                case "--beneficiary":
                case "-b": {
                    cmd.matcher().matchEntry((v) -> d.runLater.add(t -> t.setBeneficiary(v.stringValue()))).anyMatch();
                    break;
                }
                case "--company":
                case "--via":
                case "-c": {
                    cmd.matcher().matchEntry((v) -> d.runLater.add(t -> t.setCompany(v.stringValue()))).anyMatch();
                    break;
                }
                case "--day1":
                case "-1": {
                    cmd.matcher().matchEntry((v) -> d.runLater.add(t -> t.setStartWeekDay(WeekDay.parse(v.stringValue())))).anyMatch();
                    break;
                }
                case "--obs":
                case "-o": {
                    cmd.matcher().matchEntry((v) -> d.runLater.add(t -> t.setObservations(v.stringValue()))).anyMatch();
                    break;
                }
                case "--merge-to": {
                    cmd.matcher().matchEntry((v) -> {
                        if (d.mergeTo != null) {
                            cmd.pushBack(v);
                            cmd.throwUnexpectedArgument();
                        } else {
                            d.mergeTo = v.stringValue();
                        }
                    }).anyMatch();
                    break;
                }
                case "++obs":
                case "+o": {
                    cmd.matcher().matchEntry((v) -> {
                        d.runLater.add(t -> {
                            String ss = t.getObservations();
                            if (ss == null) {
                                ss = "";
                            }
                            ss = ss.trim();
                            if (!ss.isEmpty()) {
                                ss += "\n";
                            }
                            ss += v;
                            ss = ss.trim();
                            t.setObservations(ss);
                        });
                    }).anyMatch();
                    break;
                }
                default: {
                    if (aa.isNonOption()) {
                        String pid = cmd.next().get().toString();
                        NProject t = findProject(pid, cmd);
                        d.projects.add(t);
                    } else {
                        cmd.throwUnexpectedArgument();
                    }
                }
            }
        }
        if (d.projects.isEmpty()) {
            cmd.throwError(NMsg.ofNtf("project name expected"));
        }
        if (cmd.isExecMode()) {
            NTexts text = NTexts.of();
            for (NProject project : d.projects) {
                for (Consumer<NProject> c : d.runLater) {
                    c.accept(project);
                }
                service.projects().updateProject(project);
                if (session.isPlainTrace()) {
                    NOut.println(NMsg.ofC("project %s (%s) updated.",
                            text.ofStyled(project.getId(), NTextStyle.primary5()),
                            text.ofStyled(project.getName(), NTextStyle.primary1())
                    ));
                }
            }
            if (d.mergeTo != null) {
                service.projects().mergeProjects(d.mergeTo, d.projects.stream().map(x -> x.getId()).toArray(String[]::new));
                if (session.isPlainTrace()) {
                    NOut.println(NMsg.ofC("projects merged to %s.",
                            NTexts.of()
                                    .ofStyled(d.mergeTo, NTextStyle.primary5())
                    ));
                }
            }
            if (d.show) {
                for (NProject t : new LinkedHashSet<>(d.projects)) {
                    runProjectShow(NCmdLine.of(new String[]{t.getId()}));
                }
            }
            if (d.list) {
                runProjectList(NCmdLine.of(new String[0]));
            }
        }
    }

    private void runProjectList(NCmdLine cmd) {
        final NRef<Predicate<NProject>> whereFilter = NRef.ofNull();
        while (cmd.hasNext()) {
            NArg aa = cmd.peek().get();
            switch (aa.key()) {
                case "-b":
                case "-beneficiary": {
                    cmd.matcher().matchEntry((v) -> {
                        Predicate<String> sp = parent.createStringFilter(v.stringValue());
                        Predicate<NProject> t = x -> sp.test(x.getBeneficiary());
                        parent.appendPredicateRef(whereFilter, t);
                    }).anyMatch();
                    break;
                }
                case "-c":
                case "-company": {
                    cmd.matcher().matchEntry((v) -> {
                        Predicate<String> sp = parent.createStringFilter(v.stringValue());
                        Predicate<NProject> t = x -> sp.test(x.getCompany());
                        parent.appendPredicateRef(whereFilter, t);
                    }).anyMatch();
                    break;
                }
                case "-n":
                case "--name": {
                    cmd.matcher().matchEntry((v) -> {
                        Predicate<String> sp = parent.createStringFilter(v.stringValue());
                        Predicate<NProject> t = x -> sp.test(x.getName());
                        parent.appendPredicateRef(whereFilter, t);
                    }).anyMatch();
                    break;
                }
                case "--unused": {
                    cmd.matcher().matchFlag((v) -> {
                        Predicate<NProject> t = x -> service.projects().isUsedProject(x.getId()) != v.booleanValue();
                        parent.appendPredicateRef(whereFilter, t);
                    }).anyMatch();
                    break;
                }
                case "-t":
                case "--startTime":
                case "--start-time": {
                    cmd.matcher().matchEntry((v) -> {
                        Predicate<Instant> t = new TimeParser().parseInstantFilter(v.stringValue(), false);
                        parent.appendPredicateRef(whereFilter, x -> t.test(x.getStartTime()));
                    }).anyMatch();
                    break;
                }
                default: {
                    cmd.throwUnexpectedArgument();
                }
            }
        }
        if (cmd.isExecMode()) {

            Stream<NProject> r
                    = service.projects().findProjects().filter(whereFilter.isNull() ? x -> true : whereFilter.get())
                    .sorted(
                            (x, y) -> {
                                Instant s1 = x.getStartTime();
                                Instant s2 = y.getStartTime();
                                int v = s2.compareTo(s1);
                                if (v != 0) {
                                    return v;
                                }
                                return x.getName().compareTo(y.getName());
                            }
                    );

            if (session.isPlainTrace()) {
                NMutableTableModel m = NMutableTableModel.of();
                List<NProject> lastResults = new ArrayList<>();
                int[] index = new int[1];
                r.forEach(x -> {
                    Instant st = x.getStartTime();
                    String sts = "";
                    if (st != null) {
                        LocalDateTime d = LocalDateTime.ofInstant(st, ZoneId.systemDefault());
                        sts = d.getYear() + " " + d.getMonth().toString().toLowerCase().substring(0, 3);
                    }
                    lastResults.add(x);
                    index[0]++;
                    m.newRow().addCells(
                            parent.createHashId(index[0], -1),
                            x.getId(),
                            sts,
                            x.getCompany(),
                            x.getBeneficiary(),
                            parent.getFormattedProject(x.getName() == null ? "*" : x.getName())
                    );
                });
                NApp.of().setProperty("LastResults", NScopeType.SESSION, lastResults.toArray(new NProject[0]));
                NTableFormat.of()
                        .setBorder("spaces")
                        .setValue(m).println(session.out());
            } else {
                NOut.print(r.collect(Collectors.toList()));
            }
        }
    }

    private void runProjectRemove(NCmdLine cmd) {
        NTexts text = NTexts.of();
        while (cmd.hasNext()) {
            NArg a = cmd.next().get();
            if (cmd.isExecMode()) {
                NProject t = findProject(a.toString(), cmd);
                NPrintStream out = session.out();
                if (service.projects().removeProject(t.getId())) {
                    if (session.isPlainTrace()) {
                        out.println(NMsg.ofC("project %s removed.",
                                text.ofStyled(a.toString(), NTextStyle.primary5())
                        ));
                    }
                } else {
                    out.println(NMsg.ofC("project %s %s.",
                            text.ofStyled(a.toString(), NTextStyle.primary5()),
                            text.ofStyled("not found", NTextStyle.error())
                    ));
                }
            }
        }

    }

    private void runProjectShow(NCmdLine cmd) {
        while (cmd.hasNext()) {
            NArg a = cmd.next().get();
            NProject project = findProject(a.toString(), cmd);
            NPrintStream out = session.out();
            if (project == null) {
                out.println(NMsg.ofC("```kw %s```: ```error not found```.",
                        a.toString()
                ));
            } else {
                out.println(NMsg.ofC("```kw %s```:",
                        project.getId()
                ));
                String prefix = "\t                    ";
                out.println(NMsg.ofC("\t```kw2 project name```  : %s", JobServiceCmd.formatWithPrefix(project.getName(), prefix)));
                out.println(NMsg.ofC("\t```kw2 beneficiary```   : %s", JobServiceCmd.formatWithPrefix(project.getBeneficiary(), prefix)));
                out.println(NMsg.ofC("\t```kw2 company```       : %s", JobServiceCmd.formatWithPrefix(project.getCompany(), prefix)));
                out.println(NMsg.ofC("\t```kw2 start time```    : %s", JobServiceCmd.formatWithPrefix(project.getStartTime(), prefix)));
                out.println(NMsg.ofC("\t```kw2 start week day```: %s", JobServiceCmd.formatWithPrefix(project.getStartWeekDay(), prefix)));
                out.println(NMsg.ofC("\t```kw2 observations```  : %s", JobServiceCmd.formatWithPrefix(project.getObservations(), prefix)));
            }
        }

    }

    private NProject findProject(String pid, NCmdLine cmd) {
        NProject t = null;
        if (pid.startsWith("#")) {
            int x = JobServiceCmd.parseIntOrFF(pid.substring(1));
            if (x >= 1) {
                Object lastResults = NApp.of().getProperty("LastResults",NScopeType.SESSION).orNull();
                if (lastResults instanceof NProject[] && x <= ((NProject[]) lastResults).length) {
                    t = ((NProject[]) lastResults)[x - 1];
                }
            }
        }
        if (t == null) {
            t = service.projects().getProject(pid);
        }
        if (t == null) {
            cmd.throwError(NMsg.ofC("project not found: %s", pid));
        }
        return t;
    }

    public boolean runProjectCommands(NCmdLine cmd) {
        if (cmd.next("ap", "a p", "pa", "p a", "add project", "projects add").isPresent()) {
            runProjectAdd(cmd);
            return true;
        } else if (cmd.next("pu", "up", "p u", "u p", "update project", "projects update").isPresent()) {
            runProjectUpdate(cmd);
            return true;
        } else if (cmd.next("lp", "pl", "l p", "p l", "list projects", "projects list").isPresent()) {
            runProjectList(cmd);
            return true;
        } else if (cmd.next("rp", "rmp", "pr", "prm", "r p", "rm p", "p r", "p rm", "remove project", "remove projects", "rm project", "rm projects", "projects remove").isPresent()) {
            runProjectRemove(cmd);
            return true;
        } else if (cmd.next("ps", "sp", "s p", "p s", "show project", "show projects", "projects show").isPresent()) {
            runProjectShow(cmd);
            return true;
        } else if (cmd.next("p", "projects").isPresent()) {
            if (cmd.next("--help").isPresent()) {
                parent.showCustomHelp("njob-projects");
            } else {
                runProjectList(cmd);
            }
            return true;
        }
        return false;
    }

}
