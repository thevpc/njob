package net.thevpc.nuts.toolbox.njob;

import net.thevpc.nuts.app.NApp;
import net.thevpc.nuts.core.NSession;
import net.thevpc.nuts.elem.NElementDescribables;
import net.thevpc.nuts.elem.NElementParser;
import net.thevpc.nuts.elem.NElementWriter;
import net.thevpc.nuts.platform.NStoreType;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.toolbox.njob.model.Id;
import net.thevpc.nuts.toolbox.njob.model.NJob;
import net.thevpc.nuts.toolbox.njob.model.NProject;
import net.thevpc.nuts.toolbox.njob.model.NTask;
import net.thevpc.nuts.util.NFunction;
import net.thevpc.nuts.util.NPredicate;

import java.lang.reflect.Field;
import java.util.UUID;
import java.util.stream.Stream;

public class NJobConfigStore {
    private NPath dbPath;

    public NJobConfigStore(NSession session) {
        //ensure we always consider the latest config version
        dbPath = NApp.of().getVersionFolder(NStoreType.CONF, NJobConfigVersions.CURRENT)
                .resolve("db");
    }

    private Field getKeyField(Class o) {
        for (Field declaredField : o.getDeclaredFields()) {
            Id a = declaredField.getAnnotation(Id.class);
            if (a != null) {
                declaredField.setAccessible(true);
                return declaredField;
            }
        }
        throw new RuntimeException("missing @Id field");
    }

    private Object getKey(Object o) {
        try {
            return getKeyField(o.getClass()).get(o);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private String getEntityName(Class o) {
        return o.getSimpleName().toLowerCase();
    }

    private NPath getObjectFile(Object o) {
        return getFile(getEntityName(o.getClass()), getKey(o));
    }

    private NPath getFile(String entityName, Object id) {
        return dbPath.resolve(entityName).resolve(id + ".json");
    }

    public <T> Stream<T> search(Class<T> type) {
        NPath f = getFile(getEntityName(type), "any").getParent();
        NFunction<NPath, T> parse = NFunction.of((NPath x) -> NElementParser.ofJson().parse(x, type)).redescribe(NElementDescribables.ofDesc("parse"));
        return f.stream().filter(
                        NPredicate.of((NPath x) -> x.isRegularFile() && x.getName().endsWith(".json"))
                                .redescribe(NElementDescribables.ofDesc("isRegularFile() && matches(*.json" + ")"))
                )
                .map(parse)
                .filterNonNull().stream();
    }

    public <T> T load(Class<T> type, Object id) {
        NPath f = getFile(getEntityName(type), id);
        if (f.exists()) {
            return NElementParser.ofJson().parse(f, type);
        }
        return null;
    }

    public void store(Object o) {
        if (o instanceof NJob) {
            NJob j = (NJob) o;
            String ii = j.getId();
            if (ii == null) {
                j.setId(generateId(NJob.class));
            }
        } else if (o instanceof NTask) {
            NTask j = (NTask) o;
            String ii = j.getId();
            if (ii == null) {
                j.setId(generateId(NTask.class));
            }
        } else if (o instanceof NProject) {
            NProject j = (NProject) o;
            String ii = j.getId();
            if (ii == null) {
                j.setId(generateId(NProject.class));
            }
        }
        NPath objectFile = getObjectFile(o);
        objectFile.mkParentDirs();

        NElementWriter.ofJson().setCompact(false).write(o, objectFile);
    }

    public String generateId(Class clz) {
//        SimpleDateFormat yyyyMMddHHmmssSSS = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        while (true) {
            //test until we reach next millisecond
            String nid = UUID.randomUUID().toString();
//                    yyyyMMddHHmmssSSS.format(new Date());
            NPath f = getFile(getEntityName(clz), nid);
            if (!f.exists()) {
                return nid;
            }
        }
    }

    public boolean delete(Class entityName, Object id) {
        return delete(getEntityName(entityName), id);
    }

    public boolean delete(String entityName, Object id) {
        NPath f = getFile(entityName, id);
        if (f.exists()) {
            f.delete();
            return true;
        }
        return false;
    }
}
