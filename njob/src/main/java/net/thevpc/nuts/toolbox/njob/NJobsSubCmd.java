package net.thevpc.nuts.toolbox.njob;

import net.thevpc.nuts.*;
import net.thevpc.nuts.cmdline.NArg;
import net.thevpc.nuts.cmdline.NCmdLine;
import net.thevpc.nuts.format.NMutableTableModel;
import net.thevpc.nuts.format.NTableFormat;
import net.thevpc.nuts.io.NPrintStream;
import net.thevpc.nuts.spi.NScopeType;
import net.thevpc.nuts.text.NText;
import net.thevpc.nuts.text.NTextArt;
import net.thevpc.nuts.text.NTextStyle;
import net.thevpc.nuts.text.NTexts;
import net.thevpc.nuts.toolbox.njob.model.*;
import net.thevpc.nuts.toolbox.njob.time.*;
import net.thevpc.nuts.util.NMsg;
import net.thevpc.nuts.util.NRef;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NJobsSubCmd {

    private final JobService service;
    private final NSession session;
    private final JobServiceCmd parent;

    public NJobsSubCmd(JobServiceCmd parent) {
        this.parent = parent;
        this.session = parent.session;
        this.service = parent.service;
    }

    public void runJobAdd(NCmdLine cmd) {
        NJob t = new NJob();
        NRef<Boolean> list = NRef.of(false);
        NRef<Boolean> show = NRef.of(false);
        while (cmd.hasNext()) {
            NArg aa = cmd.peek().get();
            switch (aa.key()) {
                case "--list":
                case "-l": {
                    cmd.matcher().matchFlag((v)->list.set(true)).anyMatch();
                    break;
                }
                case "--show":
                case "-s": {
                    cmd.matcher().matchFlag((v)->show.set(true)).anyMatch();
                    break;
                }
                case "--time":
                case "--on":
                case "--start":
                case "-t": {
                    cmd.matcher().matchEntry((v)->t.setStartTime(new TimeParser().parseInstant(v.stringValue(), false))).anyMatch();
                    break;
                }
                case "--at": {
                    cmd.matcher().matchEntry((v)->t.setStartTime(new TimeParser().setTimeOnly(true).parseInstant(v.stringValue(), false))).anyMatch();
                    break;
                }
                case "--for":
                case "--project":
                case "-p": {
                    cmd.matcher().matchEntry((v)->t.setProject(v.stringValue())).anyMatch();
                    break;
                }
                case "--obs":
                case "-o": {
                    cmd.matcher().matchEntry((v)->t.setObservations(v.stringValue())).anyMatch();
                    break;
                }
                case "--duration":
                case "-d": {
                    cmd.matcher().matchEntry((v)->t.setDuration(TimePeriod.parse(v.stringValue(), false))).anyMatch();
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
            service.jobs().addJob(t);
            if (session.isPlainTrace()) {
                NOut.println(NMsg.ofC("job %s (%s) added.",
                        NText.ofStyled(t.getId(), NTextStyle.primary5()),
                        t.getName()
                ));
            }
            if (show.get()) {
                runJobShow(NCmdLine.of(new String[]{t.getId()}));
            }
            if (list.get()) {
                runJobList(NCmdLine.of(new String[0]));
            }
        }
    }

    public void runJobUpdate(NCmdLine cmd) {
        class Data {
            List<NJob> jobs = new ArrayList<>();
            boolean list = false;
            boolean show = false;
        }
        Data d = new Data();
        List<Consumer<NJob>> runLater = new ArrayList<>();
        while (cmd.hasNext()) {
            NArg a = cmd.peek().get();
            switch (a.key()) {
                case "--list":
                case "-l": {
                    cmd.matcher().matchFlag((v) -> d.list = v.booleanValue()).anyMatch();
                    break;
                }
                case "--show":
                case "-s": {
                    cmd.matcher().matchFlag((v) -> d.show = v.booleanValue()).anyMatch();
                    break;
                }
                case "--start": {
                    cmd.matcher().matchEntry((v) -> {
                        Instant vv = new TimeParser().parseInstant(v.stringValue(), false);
                        runLater.add(t -> t.setStartTime(vv));
                    }).anyMatch();
                    break;
                }
                case "-t":
                case "--on": {
                    cmd.matcher().matchEntry((v) -> {
                        runLater.add(t -> t.setStartTime(TimePeriod.parseOpPeriodAsInstant(v.stringValue(), t.getStartTime(), true)));
                    }).anyMatch();
                    break;
                }
                case "--at": {
                    cmd.matcher().matchEntry((v) -> {
                        Instant vv = new TimeParser().setTimeOnly(true).parseInstant(v.stringValue(), false);
                        runLater.add(t -> t.setStartTime(vv));
                    }).anyMatch();
                    break;
                }
                case "-d":
                case "--duration": {
                    cmd.matcher().matchEntry((v) -> {
                        TimePeriod vv = TimePeriod.parse(v.stringValue(), false);
                        runLater.add(t -> t.setDuration(vv));
                    }).anyMatch();
                    break;
                }
                case "-n":
                case "--name": {
                    cmd.matcher().matchEntry((v) -> {
                        runLater.add(t -> t.setName(v.stringValue()));

                    }).anyMatch();
                    break;
                }
                case "-p":
                case "--project": {
                    cmd.matcher().matchEntry((v) -> {
                        runLater.add(t -> t.setProject(v.stringValue()));
                    }).anyMatch();
                    break;
                }
                case "-o":
                case "--obs": {
                    cmd.matcher().matchEntry((v) -> {
                        runLater.add(t -> t.setObservations(v.stringValue()));
                    }).anyMatch();
                    break;
                }
                case "-o+":
                case "--obs+":
                case "+obs": {
                    cmd.matcher().matchEntry((v) -> {
                        runLater.add(t -> {
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
                    if (a.isNonOption()) {
                        NJob t = findJob(cmd.next().get().toString(), cmd);
                        d.jobs.add(t);
                    } else {
                        cmd.throwUnexpectedArgument();
                    }
                }
            }
        }
        if (d.jobs.isEmpty()) {
            cmd.throwError(NMsg.ofNtf("job id expected"));
        }
        if (cmd.isExecMode()) {
            for (NJob job : d.jobs) {
                for (Consumer<NJob> c : runLater) {
                    c.accept(job);
                }
            }
            NTexts text = NTexts.of();
            for (NJob job : new LinkedHashSet<>(d.jobs)) {
                service.jobs().updateJob(job);
                if (session.isPlainTrace()) {
                    session.out().println(NMsg.ofC("job %s (%s) updated.",
                            text.ofStyled(job.getId(), NTextStyle.primary5()),
                            text.ofStyled(job.getName(), NTextStyle.primary1())
                    ));
                }
            }
            if (d.show) {
                for (NJob t : new LinkedHashSet<>(d.jobs)) {
                    runJobList(NCmdLine.of(new String[]{t.getId()}));
                }
            }
            if (d.list) {
                runJobList(NCmdLine.of(new String[0]));
            }
        }
    }

    public boolean runJobCommands(NCmdLine cmd) {
        if (cmd.next("aj", "ja", "a j", "j a", "add job", "jobs add").isPresent()) {
            runJobAdd(cmd);
            return true;
        } else if (cmd.next("lj", "jl", "l j", "j l", "list jobs", "jobs list").isPresent()) {
            runJobList(cmd);
            return true;
        } else if (cmd.next("rj", "jr", "jrm", "rmj", "j rm", "rm j", "j r", "r j", "remove job", "remove jobs", "jobs remove").isPresent()) {
            runJobRemove(cmd);
            return true;
        } else if (cmd.next("uj", "ju", "j u", "u j", "update job", "update jobs", "jobs update", "jobs update").isPresent()) {
            runJobUpdate(cmd);
            return true;
        } else if (cmd.next("js", "sj", "j s", "s j", "show job", "show jobs", "jobs show").isPresent()) {
            runJobShow(cmd);
            return true;
        } else if (cmd.next("j", "jobs").isPresent()) {
            if (cmd.next("--help").isPresent()) {
                parent.showCustomHelp("njob-jobs");
            } else {
                runJobList(cmd);
            }
            return true;
        } else {
            return false;
        }
    }

    private void runJobRemove(NCmdLine cmd) {
        NTexts text = NTexts.of();
        while (cmd.hasNext()) {
            NArg a = cmd.next().get();
            NJob t = findJob(a.toString(), cmd);
            if (cmd.isExecMode()) {
                if (service.jobs().removeJob(t.getId())) {
                    if (session.isPlainTrace()) {
                        session.out().println(NMsg.ofC("job %s removed.",
                                text.ofStyled(a.toString(), NTextStyle.primary5())
                        ));
                    }
                } else {
                    session.out().println(NMsg.ofC("job %s %s.",
                            text.ofStyled(a.toString(), NTextStyle.primary5()),
                            text.ofStyled("not found", NTextStyle.error())
                    ));
                }
            }
        }

    }

    private void runJobShow(NCmdLine cmd) {
        while (cmd.hasNext()) {
            NArg a = cmd.next().get();
            if (cmd.isExecMode()) {
                NJob job = findJob(a.toString(), cmd);
                NPrintStream out = session.out();
                if (job == null) {
                    out.println(NMsg.ofC("```kw %s```: ```error not found```.",
                            a.toString()
                    ));
                } else {
                    out.println(NMsg.ofC("```kw %s```:",
                            job.getId()
                    ));
                    String prefix = "\t                    ";
                    out.println(NMsg.ofC("\t```kw2 job name```      : %s:", JobServiceCmd.formatWithPrefix(job.getName(), prefix)));
                    String project = job.getProject();
                    NProject p = service.projects().getProject(project);
                    if (project == null || project.length() == 0) {
                        out.println(NMsg.ofC("\t```kw2 project```       : %s", ""));
                    } else {
                        out.println(NMsg.ofC("\t```kw2 project```       : %s (%s)", project, JobServiceCmd.formatWithPrefix(p == null ? "?" : p.getName(), prefix)));
                    }
                    out.println(NMsg.ofC("\t```kw2 duration```      : %s", JobServiceCmd.formatWithPrefix(job.getDuration(), prefix)));
                    out.println(NMsg.ofC("\t```kw2 start time```    : %s", JobServiceCmd.formatWithPrefix(job.getStartTime(), prefix)));
                    out.println(NMsg.ofC("\t```kw2 duration extra```: %s", JobServiceCmd.formatWithPrefix(job.getInternalDuration(), prefix)));
                    out.println(NMsg.ofC("\t```kw2 observations```  : %s", JobServiceCmd.formatWithPrefix(job.getObservations(), prefix)));
                }
            }
        }

    }

    private void runJobList(NCmdLine cmd) {
        class Data {
            TimespanPattern hoursPerDay = TimespanPattern.WORK;
            int count = 100;
            NJobGroup groupBy = null;
            ChronoUnit countType = null;
            ChronoUnit timeUnit = null;
            Predicate<NJob> whereFilter = null;
        }
        Data d = new Data();
        while (cmd.hasNext()) {
            NArg a = cmd.peek().get();
            switch (a.key()) {
                case "-w":
                case "--weeks": {
                    cmd.matcher().matchEntry((v) -> {
                        d.countType = ChronoUnit.WEEKS;
                        d.count = v.intValue();
                    }).anyMatch();
                    break;
                }
                case "-m":
                case "--months": {
                    cmd.matcher().matchEntry((v) -> {
                        d.countType = ChronoUnit.MONTHS;
                        d.count = v.intValue();
                    }).anyMatch();

                    break;
                }
                case "-l": {
                    cmd.matcher().matchEntry((v) -> {
                        d.countType = null;
                        d.count = v.intValue();
                    }).anyMatch();

                    break;
                }
                case "-u":
                case "--unit": {
                    cmd.matcher().matchEntry((v) -> {
                        d.timeUnit = TimePeriod.parseUnit(v.stringValue(), false);
                    }).anyMatch();
                    break;
                }
                case "-g":
                case "--group":
                case "--groupBy":
                case "--groupby":
                case "--group-by": {
                    cmd.matcher().matchEntry((v) -> {
                        switch (v.stringValue()) {
                            case "p":
                            case "project": {
                                d.groupBy = NJobGroup.PROJECT_NAME;
                                break;
                            }
                            case "n":
                            case "name": {
                                d.groupBy = NJobGroup.NAME;
                                break;
                            }
                            case "s":
                            case "summary": {
                                d.groupBy = NJobGroup.SUMMARY;
                                break;
                            }
                            default: {
                                cmd.pushBack(v).throwUnexpectedArgument(NMsg.ofPlain("invalid value"));
                            }
                        }
                    }).anyMatch();
                    break;
                }
                case "-p":
                case "--project": {
                    cmd.matcher().matchEntry((v) -> {
                        Predicate<String> sp = parent.createProjectFilter(v.stringValue());
                        Predicate<NJob> t = x -> sp.test(x.getProject());
                        d.whereFilter = parent.appendPredicate(d.whereFilter, t);
                    }).anyMatch();
                    break;
                }
                case "--name": {
                    cmd.matcher().matchEntry((v) -> {
                        Predicate<String> sp = parent.createStringFilter(v.stringValue());
                        Predicate<NJob> t = x -> sp.test(x.getName());
                        d.whereFilter = parent.appendPredicate(d.whereFilter, t);
                    }).anyMatch();
                    break;
                }
                case "-b":
                case "--beneficiary": {
                    cmd.matcher().matchEntry((v) -> {
                        Predicate<String> sp = parent.createStringFilter(v.stringValue());
                        Predicate<NJob> t = x -> {
                            NProject project = service.projects().getProject(x.getProject());
                            return sp.test(project == null ? "" : project.getBeneficiary());
                        };
                        d.whereFilter = parent.appendPredicate(d.whereFilter, t);
                    }).anyMatch();
                    break;
                }
                case "-c":
                case "--company": {
                    cmd.matcher().matchEntry((v) -> {
                        Predicate<String> sp = parent.createStringFilter(v.stringValue());
                        Predicate<NJob> t = x -> {
                            NProject project = service.projects().getProject(x.getProject());
                            return sp.test(project == null ? "" : project.getCompany());
                        };
                        d.whereFilter = parent.appendPredicate(d.whereFilter, t);
                    }).anyMatch();
                    break;
                }
                case "-d":
                case "--duration": {
                    cmd.matcher().matchEntry((v) -> {
                        Predicate<TimePeriod> p = TimePeriod.parseFilter(v.stringValue(), false);
                        Predicate<NJob> t = x -> p.test(x.getDuration());
                        d.whereFilter = parent.appendPredicate(d.whereFilter, t);
                    }).anyMatch();
                    break;
                }
                case "-t":
                case "--startTime":
                case "--start-time": {
                    cmd.matcher().matchEntry((v) -> {
                        Predicate<Instant> t = new TimeParser().parseInstantFilter(v.stringValue(), false);
                        d.whereFilter = parent.appendPredicate(d.whereFilter, x -> t.test(x.getStartTime()));

                    }).anyMatch();
                    break;
                }
                default: {
                    cmd.throwUnexpectedArgument();
                }
            }
        }
        if (cmd.isExecMode()) {
            Stream<NJob> r = service.jobs().findLastJobs(null, d.count, d.countType, d.whereFilter, d.groupBy, d.timeUnit, d.hoursPerDay);
            ChronoUnit timeUnit0 = d.timeUnit;
            if (session.isPlainTrace()) {
                NMutableTableModel m = NMutableTableModel.of();
                NJobGroup finalGroupBy = d.groupBy;
                List<NJob> lastResults = new ArrayList<>();
                int[] index = new int[1];
                r.forEach(x -> {
                    NText durationString = NText.ofStyled(String.valueOf(timeUnit0 == null ? x.getDuration() : x.getDuration().toUnit(timeUnit0, d.hoursPerDay)), NTextStyle.keyword());
                    index[0]++;
                    lastResults.add(x);
                    m.newRow().addCells(
                            (finalGroupBy != null)
                                    ? new NText[]{
                                    NText.of(parent.createHashId(index[0], -1)),
                                    NText.of(parent.getFormattedDate(x.getStartTime())),
                                    NText.of(durationString),
                                    parent.getFormattedProject(x.getProject() == null ? "*" : x.getProject()),
                                    NText.of(x.getName())

                            } : new NText[]{
                                    NText.of(parent.createHashId(index[0], -1)),
                                    NText.ofStyled(x.getId(), NTextStyle.pale()),
                                    NText.of(parent.getFormattedDate(x.getStartTime())),
                                    NText.of(durationString),
                                    parent.getFormattedProject(x.getProject() == null ? "*" : x.getProject()),
                                    NText.of(x.getName())
                            }
                    );
                });
                NApp.of().setProperty("LastResults", NScopeType.SESSION, lastResults.toArray(new NJob[0]));
                NOut.println(
                        NTextArt.of().getTableRenderer("table:spaces")
                                .get().render(m)
                );
            } else {
                session.out().print(r.collect(Collectors.toList()));
            }
        }
    }

    private NJob findJob(String pid, NCmdLine cmd) {
        NJob t = null;
        if (pid.startsWith("#")) {
            int x = JobServiceCmd.parseIntOrFF(pid.substring(1));
            if (x >= 1) {
                Object lastResults = NApp.of().getProperty("LastResults",NScopeType.SESSION).orNull();
                if (lastResults instanceof NJob[] && x <= ((NJob[]) lastResults).length) {
                    t = ((NJob[]) lastResults)[x - 1];
                }
            }
        }
        if (t == null) {
            t = service.jobs().getJob(pid);
        }
        if (t == null) {
            cmd.throwError(NMsg.ofC("job not found: %s", pid));
        }
        return t;
    }
}
