package cloak.idea

import com.intellij.openapi.application.PathManager
import java.io.File
import java.nio.file.Paths



//TODO: plan:
// - Maintain a singular git repository that exists in github.com at all times.
// - Whenever the first rename happens, the repository will be cloned to a local directory.
// - Whenever the user makes a rename, the git repository will switch to his own branch or create one as needed.
//   - The git repository will be modified with the change proposed,
//     and then the changes will be immediately commited locally
//   - The author can specify an explanation to the rename.
//   - The user will recieve feedback on his rename
//   - Explanation will be stored in a file in the branch and deleted when the pull request is made.
//   - The input will be validated.
// - When a person wishes to submit his renames, he must specify a name for the mappings set,
//   and an author in the form of a github link,
//   and a new branch will be created with the changes he has made, named with the name of the mappings set.
//   - The changes will be pushed to the remote
//   - A pull request will then be immediately made from the created branch to the latest branch of yarn,
//     or, to a branch he will specify.
//   - His original branch will be updated to the latest version of yarn.
//   - At any time, he may do renames while specifying the pull request ID, and changes will be made to that PR specifically.
//   - The PR will specify the author has collaborated in making the PR.
//   - The author can be "anonymous".
//   - The pull request will provide a detailed list of changes in the body in an easy-to-read format,
//     together with the explanations provided during renaming.
//   - The master branch gets updated manually every so often.
// - 'Unnamed X in Y' command: list all unnamed classes/methods/fields/parameters


//TODO: Version 2:
// - Users may message the bot directly.
// - "Stats" command - provide statistics on how much stuff is named
// - Users may register their github name and email and bind it to their discord ID.
// This will be stored in a database and they will be given full credit for commits made in their name.
// - Branches will be stored in a database with the date they were last modified.
//    - Whenever a change is made, the bot will check if it conflicts with any branches that have recent changes (a week or so)