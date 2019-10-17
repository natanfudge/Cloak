// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package knit;

import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.rename.RenameHandler;
import com.intellij.util.NullableFunction;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Dmitry Batkovich
 */
public class ResourceBundleFromEditorRenameHandler implements RenameHandler {

  @Override
  public boolean isAvailableOnDataContext(@NotNull DataContext dataContext) {
    return true;
  }

  @Override
  public boolean isRenaming(@NotNull DataContext dataContext) {
    return false;
  }

  @Override
  public void invoke(final @NotNull Project project, Editor editor, final PsiFile file, DataContext dataContext) {
  }

  @Override
  public void invoke(@NotNull Project project, @NotNull PsiElement[] elements, DataContext dataContext) {
    invoke(project, null, null, dataContext);
  }

}
