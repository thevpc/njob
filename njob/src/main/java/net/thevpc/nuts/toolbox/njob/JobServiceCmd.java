package net.thevpc.nuts.toolbox.njob;

import net.thevpc.nuts.*;
import net.thevpc.nuts.cmdline.NCmdLine;
import net.thevpc.nuts.cmdline.NCmdLineHistory;

import net.thevpc.nuts.io.NIO;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.io.NSystemTerminal;
import net.thevpc.nuts.text.*;
import net.thevpc.nuts.toolbox.njob.model.*;
import net.thevpc.nuts.toolbox.njob.time.TimeFormatter;
import net.thevpc.nuts.util.NMsg;
import net.thevpc.nuts.util.NRef;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JobServiceCmd {

    protected JobService service;
    protected NSession session;
    private NJobsSubCmd jobs;
    private NTasksSubCmd tasks;
    private NProjectsSubCmd projects;

    public JobServiceCmd(NSession session) {
        this.session = session;
        this.service = new JobService(session);
        jobs = new NJobsSubCmd(this);
        tasks = new NTasksSubCmd(this);
        projects = new NProjectsSubCmd(this);
    }

    protected static String formatWithPrefix(Object value, String prefix) {
        if (prefix == null) {
            prefix = "";
        }
        if (value == null) {
            value = "";
        }
        if (value instanceof Instant) {
            value = LocalDateTime.ofInstant((Instant) value, ZoneId.systemDefault());
        }
        return Arrays.stream(value.toString().split("(\n|\r\n)")).collect(Collectors.joining("\n" + prefix));
    }

    public static int parseIntOrFF(String s) {
        if (s == null || s.isEmpty()) {
            return -1;
        }
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return -1;
        }
    }

    public boolean runCommands(NCmdLine cmd) {
        if (projects.runProjectCommands(cmd)) {
            return true;
        }
        if (jobs.runJobCommands(cmd)) {
            return true;
        }
        if (tasks.runTaskCommands(cmd)) {
            return true;
        }
        if (cmd.next("summary").isPresent()) {
            runSummary(cmd);
            return true;
        } else if (cmd.next("help").isPresent()) {
            for (String s : new String[]{"jobs", "projects", "tasks"}) {
                if (cmd.isExecMode()) {
                    showCustomHelp("njob-" + s);
                }
                return true;
            }
            if (cmd.isExecMode()) {
                showCustomHelp("njob");
            }
            return true;
        }
        return false;
    }

    private void runSummary(NCmdLine cmd) {
        if (cmd.isExecMode()) {
            long projectsCount = service.projects().findProjects().count();
            long tasksCount = service.tasks().findTasks(NTaskStatusFilter.OPEN, null, -1, null, null, null, null, null).count();
            long jobsCount = service.jobs().findMonthJobs(null).count();
            long allJobsCount = service.jobs().findLastJobs(null, -1, null, null, null, null, null).count();
            NTexts text = NTexts.of();
            NOut.print(NMsg.ofC("%s open task%s\n", text.ofStyled("" + tasksCount, NTextStyle.primary1()), tasksCount == 1 ? "" : "s"));
            NOut.print(NMsg.ofC("%s job%s %s\n", text.ofStyled("" + allJobsCount, NTextStyle.primary1()), allJobsCount == 1 ? "" : "s",
                    allJobsCount == 0 ? ""
                            : text.ofBuilder()
                            .append("(")
                            .append("" + jobsCount, NTextStyle.primary1())
                            .append(" this month)")
            ));
            NOut.print(NMsg.ofC("%s project%s\n", text.ofStyled("" + projectsCount, NTextStyle.primary1()), projectsCount == 1 ? "" : "s"));
        }
    }

    protected void showCustomHelp(String name) {
        NTexts text = NTexts.of();
        NPath p = NPath.of("classpath:/net/thevpc/nuts/toolbox/" + name + ".ntf");
        NOut.println(
                text.transform(text.parser().parse(p), new NTextTransformConfig()
                        .setCurrentDir(p.getParent())
                        .setImportClassLoader(getClass().getClassLoader())
                        .setRootLevel(1)
                        .setProcessAll(true)
                        .setNormalize(true)
                )
        );
    }

    protected NText getFormattedProject(String projectName) {
        NTextBuilder builder = NTextBuilder.of();
        builder.getStyleGenerator()
                .setIncludeForeground(true)
                .setUsePaletteColors();
        return builder.appendHashStyle(projectName).immutable();
    }

    protected String getFormattedDate(Instant x) {
        if (x == null) {
            return "?";
        }
        return new TimeFormatter().format(x.atZone(ZoneId.systemDefault()).toLocalDateTime());
//        String s = x.atZone(ZoneId.systemDefault()).toString() + " " +
//                x.atZone(ZoneId.systemDefault()).getDayOfWeek().toString().toLowerCase().substring(0, 3);
//        return s;
    }

    protected NText getCheckedString(Boolean x) {
        if (x == null) {
            return NText.ofPlain("");
        }
        if (x) {
            return NText.ofPlain("\u2611");
        } else {
            return NText.ofPlain("\u25A1");
        }
    }

    protected NText getPriorityString(NPriority x) {
        if (x == null) {
            return NText.ofPlain("N");
        }
        switch (x) {
            case NONE:
                return NText.ofStyled("0", NTextStyle.pale());
            case LOW:
                return NText.ofStyled("L", NTextStyle.pale());
            case NORMAL:
                return NText.ofPlain("N");
            case MEDIUM:
                return NText.ofStyled("M", NTextStyle.primary1());
            case URGENT:
                return NText.ofStyled("U", NTextStyle.primary2());
            case HIGH:
                return NText.ofStyled("H", NTextStyle.primary3());
            case CRITICAL:
                return NText.ofStyled("C", NTextStyle.fail());
        }
        return NText.ofPlain("?");
    }

    protected NText getStatusString(NTaskStatus x) {
        NTexts text = NTexts.of();
        if (x == null) {
            return text.ofPlain("*");
        }
        switch (x) {
            case TODO:
                return text.ofPlain("\u24c9");
            case DONE:
                return text.ofStyled("\u2611", NTextStyle.success());
            case WIP:
                return text.ofStyled("\u24CC", NTextStyle.primary1());
            case CANCELLED:
                return text.ofStyled("\u2718", NTextStyle.fail());
        }
        return text.ofPlain("?");
    }

    private NText getFlagString(String x, int index) {
        switch (index) {
            case 1:
                return NText.ofStyled(x, NTextStyle.primary1());
            case 2:
                return NText.ofStyled(x, NTextStyle.primary2());
            case 3:
                return NText.ofStyled(x, NTextStyle.primary3());
            case 4:
                return NText.ofStyled(x, NTextStyle.primary4());
            case 5:
                return NText.ofStyled(x, NTextStyle.primary5());
        }
        throw new NIllegalArgumentException(NMsg.ofC("invalid index %s", index));
    }

    protected NText getFlagString(NFlag x) {
        if (x == null) {
            x = NFlag.NONE;
        }
        switch (x) {
            case NONE:
                return NText.ofPlain("\u2690");

            case STAR1:
                return getFlagString("\u2605", 1);
            case STAR2:
                return getFlagString("\u2605", 2);
            case STAR3:
                return getFlagString("\u2605", 3);
            case STAR4:
                return getFlagString("\u2605", 4);
            case STAR5:
                return getFlagString("\u2605", 5);

            case FLAG1:
                return getFlagString("\u2691", 1);
            case FLAG2:
                return getFlagString("\u2691", 2);
            case FLAG3:
                return getFlagString("\u2691", 3);
            case FLAG4:
                return getFlagString("\u2691", 4);
            case FLAG5:
                return getFlagString("\u2691", 5);

            case KING1:
                return getFlagString("\u265A", 1);
            case KING2:
                return getFlagString("\u265A", 2);
            case KING3:
                return getFlagString("\u265A", 3);
            case KING4:
                return getFlagString("\u265A", 4);
            case KING5:
                return getFlagString("\u265A", 5);

            case HEART1:
                return getFlagString("\u2665", 1);
            case HEART2:
                return getFlagString("\u2665", 2);
            case HEART3:
                return getFlagString("\u2665", 3);
            case HEART4:
                return getFlagString("\u2665", 4);
            case HEART5:
                return getFlagString("\u2665", 5);

            case PHONE1:
                return getFlagString("\u260E", 1);
            case PHONE2:
                return getFlagString("\u260E", 2);
            case PHONE3:
                return getFlagString("\u260E", 3);
            case PHONE4:
                return getFlagString("\u260E", 4);
            case PHONE5:
                return getFlagString("\u260E", 5);
        }
        return NText.ofPlain("[" + x.toString().toLowerCase() + "]");
    }

    protected <T> void appendPredicateRef(NRef<Predicate<T>> whereFilter, Predicate<T> t) {
        whereFilter.set(appendPredicate(whereFilter.get(),t));
    }

    protected <T> Predicate<T> appendPredicate(Predicate<T> whereFilter, Predicate<T> t) {
        if (whereFilter == null) {
            whereFilter = t;
        } else {
            whereFilter = whereFilter.and(t);
        }
        return whereFilter;
    }

    protected Predicate<String> createStringFilter(String s) {
        if (s.length() > 0 && s.startsWith("/") && s.endsWith("/")) {
            Pattern pattern = Pattern.compile(s);
            return x -> pattern.matcher(x == null ? "" : x).matches();
        }
        if (s.length() > 0 && s.contains("*")) {
            Pattern pattern = Pattern.compile(JobService.wildcardToRegex(s));
            return x -> pattern.matcher(x == null ? "" : x).matches();
        }
        return x -> s.equals(x == null ? "" : x);
    }

    public void runInteractive(NCmdLine cmdLine) {
        NSystemTerminal.enableRichTerm();
        NIO.of().getSystemTerminal()
                .setCommandAutoCompleteResolver(new JobAutoCompleter(session.getWorkspace()))
                .setCommandHistory(
                        NCmdLineHistory.of()
                                .setPath(NApp.of().getVarFolder().resolve("njob-history.hist"))
                );
        NWorkspace.of().setProperty(JobServiceCmd.class.getName(), this);

//        session.setTerminal(
//                session.io().term().createTerminal(
//                session.io().term().getSystemTerminal(), 
//                        session
//        ));
        NTexts text = NTexts.of();

        NId appId = NApp.of().getId().get();
        NOut.print(NMsg.ofC(
                "%s interactive mode. type %s to quit.%n",
                text.ofStyled(appId.getArtifactId() + " " + appId.getVersion(), NTextStyle.primary1()),
                text.ofStyled("q", NTextStyle.error())
        ));
        InputStream in = session.getTerminal().in();
        Exception lastError = null;
        while (true) {
            String line = null;
            try {
                line = session.getTerminal().readLine(NMsg.ofPlain("> "));
            } catch (NoSuchElementException e) {
            }
            if (line == null) {
                break;
            }
            //line=line.trim();
            if (line.isEmpty()) {
                //
            } else if (line.trim().equals("q") || line.trim().equals("quit") || line.trim().equals("exit")) {
                break;
            } else if (line.trim().equals("err") || line.trim().equals("show-error") || line.trim().equals("show error")) {
                if (lastError != null) {
                    lastError.printStackTrace(NOut.asPrintStream());
                }
            } else {
                NCmdLine cmd = NCmdLine.parseDefault(line).get();
                cmd.setCommandName(appId.getArtifactId());
                try {
                    lastError = null;
                    boolean b = runCommands(cmd);
                    if (!b) {
                        NOut.println("```error command not found```");
                    }
                } catch (Exception ex) {
                    lastError = ex;
                    String m = ex.getMessage();
                    if (m == null) {
                        m = ex.toString();
                    }
                    session.err().print(NMsg.ofC("```error ERROR: %s```\n", m));
                }
            }
        }
    }

    public Predicate<String> createProjectFilter(String s) {
        if (service.isIdFormat(s)) {
            return createStringFilter(s);
        } else {
            Predicate<String> sp = createStringFilter(s);
            return x -> {
                NProject y = service.projects().getProject(x);
                return y != null && sp.test(y.getName());
            };
        }
    }

    protected String createHashId(int value, int maxValues) {
//        DecimalFormat decimalFormat = new DecimalFormat("00");
//        return "#"+decimalFormat.format(value);
        return "#" + value;
    }
}
