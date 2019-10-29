import com.intellij.openapi.components.*;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@State(name = "IntermediaryToNamed", storages = @Storage(StoragePathMacros.CACHE_FILE))
public class ClassNameProvider implements PersistentStateComponent<ClassNameProvider.State> {

    public static class State {
        public Map<String, String> namedToIntermediary = new HashMap<>();

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            State state = (State) o;
            return Objects.equals(namedToIntermediary, state.namedToIntermediary);
        }

        @Override
        public int hashCode() {
            return Objects.hash(namedToIntermediary);
        }
    }

    public State state = new State();

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    public static ClassNameProvider getInstance() {
        return ServiceManager.getService(ClassNameProvider.class);
    }

}
