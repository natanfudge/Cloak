
import com.intellij.openapi.components.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


// this doesn't work in kotlin for some fucking reason

// So we can find methods that have intermediary descriptors while the user sees named descriptors
@State(name = "RenamedNames", storages = @Storage(StoragePathMacros.CACHE_FILE))
public class ExistingIntermediaryNamesProvider implements PersistentStateComponent<ExistingIntermediaryNamesProvider.State> {

    public static class State {
        public List<String> existingIntermediaryNames = new ArrayList<>();
        @Nullable
        public String currentVersion = null;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            State state = (State) o;
            return Objects.equals(existingIntermediaryNames, state.existingIntermediaryNames);
        }

        @Override
        public int hashCode() {
            return Objects.hash(existingIntermediaryNames);
        }
    }

    private State state = new State();

    /**
     * Must be called whenever switching to a new version
     */
    public void update(String newVersion) {
        
    }

    public boolean intermediaryNameExists(String name,String version) {
        if(state.existingIntermediaryNames.isEmpty() || !Objects.equals(state.currentVersion, version)) update(version);
        return state.existingIntermediaryNames.contains(name);
    }

    public State getState() {
        return this.state;
    }

    public void loadState(@NotNull State state) {
        this.state = state;
    }

    public static RenamedNamesProvider getInstance() {
        return ServiceManager.getService(RenamedNamesProvider.class);
    }

}