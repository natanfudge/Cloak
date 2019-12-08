# Cloak

A cloak is a single piece of fabric that you wear around you. 

## Q & A

### What is this?

Cloak is a plugin for [IntelliJ IDEA](https://www.jetbrains.com/idea/) that intends to provide various utilities to aid Fabric development. At the moment it has one sizeable feature - **Cloak Renamer**. While browsing the Minecraft code as-per-normal, Cloak allows you to right click on an identifier and rename it. While the change won't reflect on the code itself immediately, it will be registered. You can then simply press on "Submit mappings", and the changes will be PR'd to the latest branch of the fabric yarn repository. This essentially removes the entry barrier to submitting mappings, and promotes fixing small things when you see them.   
https://www.youtube.com/watch?v=mfhZQmR52_8
 
### Is this an Enigma replacement? 

I believe the usage of Cloak is much simpler than that of Enigma, and therefore an ultimate goal would be to replace Enigma. However, there are certain things Enigma can do that Cloak cannot, and therefore Enigma will stick around for now. These are detailed in the next segments. 

### - Eclipse

Cloak is only an IntelliJ plugin. **However**, Cloak is designed from the ground up to be usable on other IDEs as well. This means that to support Eclipse, only a defined set of functionalities need to be implemented that are specific to the platform. These are: a way to transform from the platform-specific AST to the (extremely simple) Cloak AST, and user interface functionalities (input boxes, notifications, etc). Since I (Fudge) do not use Eclipse myself, I do not plan to implement this on my own, but if anyone is looking to have this in Eclipse, I will gladly guide them through the process. 

### - Seeing the names themselves change, before the changes are pulled in, and also changing names that only exist in older versions

These things require a way to use new mappings in older versions, which is yet to be implemented in Fabric (but it will be at some point).

### What about VSCode? 

Since VSCode runs on Node-JS, that would mean we cannot reuse any JVM infrastructure. Because Cloak is written in Kotlin, that also compiles to Javascript, this is technically possible, but a lot of things will need to be swapped out. 

### Why Kotlin.......

I like Kotlin. It's fully interoperable with Java code, and has some nice APIs in the IDEA toolkit. You can read about a small subset of improvements Kotlin has over Java [here](https://kotlinlang.org/docs/reference/comparison-to-java.html).
It's not a dynamic language. `var` is just type inference (see, Java 10).

#### Any Contributions using Java will be accepted. 

  
  
  

### Additional usage instructions

- Rename an element by either right clicking it and selecting "Rename", or pressing `alt+F2`. 
- Submit mappings with `Tools -> Fabric -> Submit mappings` or `ctrl+alt+F2`.
- Revisit already submitted mappings with `Tools -> Fabric -> Switch Branch` or `shift+ctrl+B`, and update them by submitting mappings again.
- Delete branches to clear up space with `Tools -> Fabric -> Delete Branches` or `ctrl+alt+D`. 

