package net.thevpc.nuts.toolbox.njob;

import net.thevpc.nuts.app.NApplication;
import net.thevpc.nuts.app.NApp;
import net.thevpc.nuts.app.NAppRun;
import net.thevpc.nuts.cmdline.NArg;
import net.thevpc.nuts.cmdline.NCmdLine;
import net.thevpc.nuts.core.NSession;

@NApp
public class NJobMain {


    public static void main(String[] args) {
        NApplication.builder(args).run();
    }

    @NAppRun
    public void run() {
        NSession session = NSession.of();
        JobServiceCmd ts = new JobServiceCmd(session);
        NCmdLine cmdLine = NApplication.of().cmdLine();
        NArg a;
        while(!cmdLine.isEmpty()) {
            if (session.configureFirst(cmdLine)) {
                //
            } else if (
                    cmdLine.peek().get().toString().equals("-i")
                    ||cmdLine.peek().get().toString().equals("--interactive")
            ) {
                //interactive
                ts.runInteractive(cmdLine);
                return;
            } else if (ts.runCommands(cmdLine)) {
                //okkay
                return;
            } else {
                cmdLine.throwUnexpectedArgument();
            }
        };
        ts.runCommands(NCmdLine.of(new String[]{"summary"}));
    }

}
