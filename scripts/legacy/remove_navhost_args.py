import re

def strip_arguments():
    filepath = "feature/ui/src/main/java/com/ahmedyejam/mks/ui/MksNavHost.kt"
    with open(filepath, "r") as f:
        content = f.read()

    # Find blocks like:
    # arguments = listOf(
    #     navArgument("mistakeId") { type = NavType.LongType; defaultValue = -1L }
    # ),
    # and remove them. We can use a regex.
    # Note: the comma at the end might be there.
    
    # regex for `arguments = listOf(...)` covering multiple lines and taking care of nesting
    # We can just remove anything that matches `arguments = listOf(.*?)` where the parens match.
    # Actually, the simplest is to match `arguments\s*=\s*listOf\([^)]*\),?`
    # But some might have nested braces.
    
    # We know the arguments blocks look like:
    # arguments = listOf(navArgument(...) { ... }, navArgument(...) { ... }),
    
    # Let's remove them via regex:
    content = re.sub(r'arguments\s*=\s*listOf\([^\]]*?\),?\s*', '', content, flags=re.DOTALL)
    # the above might match too much if there's no `]`? Let's be careful.
    
    # A safer way is to match `arguments = listOf(` to the corresponding closing `)`.
    # Let's just write a parser for it.
    idx = content.find("arguments = listOf(")
    while idx != -1:
        # Find the matching parenthesis
        paren_count = 0
        end_idx = -1
        for i in range(idx + len("arguments = "), len(content)):
            if content[i] == '(':
                paren_count += 1
            elif content[i] == ')':
                paren_count -= 1
                if paren_count == 0:
                    end_idx = i
                    break
        
        if end_idx != -1:
            # Check if there is a trailing comma
            remove_end = end_idx + 1
            while remove_end < len(content) and content[remove_end] in [' ', '\n', '\r']:
                remove_end += 1
            if remove_end < len(content) and content[remove_end] == ',':
                remove_end += 1
            
            # Remove the substring
            content = content[:idx] + content[remove_end:]
        
        idx = content.find("arguments = listOf(")

    with open(filepath, "w") as f:
        f.write(content)

if __name__ == "__main__":
    strip_arguments()
