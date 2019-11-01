import cloak.idea.NewName;
import cloak.idea.ObjWrapper;
import cloak.idea.RenamedNamesProviderKt;
import cloak.mapping.rename.Name;
import com.intellij.openapi.components.*;
import com.intellij.util.xmlb.XmlSerializerUtil;
import kotlinx.serialization.json.Json;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

;
// this doesn't work in kotlin for some fucking reason

// So we can find methods that have intermediary descriptors while the user sees named descriptors
@State(name = "RenamedNames", storages = @Storage(StoragePathMacros.NON_ROAMABLE_FILE))
public class RenamedNamesProvider implements PersistentStateComponent<RenamedNamesProvider.State> {

    public static class State {
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

    private State state = new State();


    private Map<Name, NewName> renamedNames = new HashMap<>();

    public void addRenamedName(Name name, NewName renamedTo) {
        renamedNames.put(name, renamedTo);
        Json json = ObjWrapper.INSTANCE.getNamesJson();
        state.renamedNamesJson.put(json.stringify(ObjWrapper.INSTANCE.getNameSerializer(),name),
                json.stringify(ObjWrapper.INSTANCE.getNewNameSerializer(), renamedTo));
    }

    public void cleanNames() {
        renamedNames.clear();
        state.renamedNamesJson.clear();
    }

    public boolean anythingWasRenamed() {
        return !renamedNames.isEmpty();
    }


    @Nullable
    public NewName getRenameOf(Name name) {
        return renamedNames.get(name);
    }


    public State getState() {
        return this.state;
    }

    public void loadState(@NotNull State state) {
        this.state = state;
        RenamedNamesProviderKt.loadState(state,renamedNames);
        int x = 2;
    }

    public static RenamedNamesProvider getInstance() {
        return ServiceManager.getService(RenamedNamesProvider.class);
    }

}