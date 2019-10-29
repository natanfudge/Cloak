import cloak.idea.ObjWrapper;
import cloak.mapping.rename.Name;
import com.intellij.openapi.components.*;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

;
// this doesn't work in kotlin for some fucking reason

// So we can find methods that have intermediary descriptors while the user sees named descriptors
@State(name = "RenamedNames", storages = @Storage(StoragePathMacros.NON_ROAMABLE_FILE))
public class RenamedNamesProvider implements PersistentStateComponent<RenamedNamesProvider.State> {

    static class State {
        public Map<String, String> renamedNamesJson = new HashMap<>();

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            State state = (State) o;
            return Objects.equals(renamedNamesJson, state.renamedNamesJson);
        }

        @Override
        public int hashCode() {
            return Objects.hash(renamedNamesJson);
        }
    }

    State state = new State();


    Map<Name, String> renamedNames = new HashMap<>();

    public void addRenamedName(Name name, String renamedTo) {
        renamedNames.put(name, renamedTo);
        state.renamedNamesJson.put(ObjWrapper.INSTANCE.getNamesJson().stringify(ObjWrapper.INSTANCE.getNameSerializer(), name), renamedTo);
    }

    public void cleanNames() {
        renamedNames.clear();
        state.renamedNamesJson.clear();
    }

    public boolean anythingWasRenamed() {
        return !renamedNames.isEmpty();
    }


    @Nullable
    public String getRenameOf(Name name) {
        return renamedNames.get(name);
    }


    public State getState() {
        return this.state;
    }

    public void loadState(State state) {
        this.state = state;
        state.renamedNamesJson
                .forEach((k, v) -> renamedNames.put(ObjWrapper.INSTANCE.getNamesJson().parse(ObjWrapper.INSTANCE.getNameSerializer(), k), v));
    }

    public static RenamedNamesProvider getInstance() {
        return ServiceManager.getService(RenamedNamesProvider.class);
    }

}