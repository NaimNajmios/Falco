import re

file_path = r"c:\Users\NAIM\Documents\UMT\FYP\Project Repo\fleetshare\fleetshare\README.md"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# Remove emojis
# Common emojis in the text
content = content.replace("⚙️", "")
content = content.replace("✔️", "")
content = content.replace("✅", "")
content = content.replace("🚀", "")
content = content.replace("🛠️", "")
content = content.replace("📊", "")
content = content.replace("👨‍💻", "")
content = content.replace("🛡️", "")
content = content.replace("🚗", "")
content = content.replace("📱", "")
content = content.replace("💰", "")
content = content.replace("📈", "")
content = content.replace("🛠", "")
content = content.replace("⚙", "")
content = content.replace("️", "") # invisible variation selector

# Fix multiple spaces that might result from removal
content = content.replace("###  Implementation", "### Implementation")
content = content.replace("### Implementation", "### Implementation")

# Update Project Directory Structure
old_tree = """src/main/java/
├── com.najmi.fleetshare/
│   ├── FleetshareApplication.java    # Main entry point
│   ├── TestPasswordEncoder.java      # Utility for generating BCrypt passwords
│   │
│   ├── config/                       # Configuration classes"""

new_tree = """src/main/java/
├── com.najmi.fleetshare/
│   ├── FleetshareApplication.java    # Main entry point
│   ├── TestPasswordEncoder.java      # Utility for generating BCrypt passwords
│   │
│   ├── aspect/                       # Aspect-Oriented Programming (AOP)
│   │   └── LoggingAspect.java        # System-wide audit and logging aspect
│   │
│   ├── config/                       # Configuration classes"""

content = content.replace(old_tree, new_tree)

old_tree_2 = """│   ├── entity/                       # JPA Entities
│   │   ├── User.java                 # Base user entity
│   │   ├── FleetOwner.java           # Owner specific attributes
│   │   ├── Renter.java               # Renter specific attributes
│   │   ├── PlatformAdmin.java        # Admin specific attributes
│   │   └── UserRole.java             # Role enumeration
│   │
│   ├── repository/                   # Data Access Layer"""

new_tree_2 = """│   ├── entity/                       # JPA Entities
│   │   ├── User.java                 # Base user entity
│   │   ├── FleetOwner.java           # Owner specific attributes
│   │   ├── Renter.java               # Renter specific attributes
│   │   ├── PlatformAdmin.java        # Admin specific attributes
│   │   └── UserRole.java             # Role enumeration
│   │
│   ├── exception/                    # Global Exception Handling
│   │   └── RegistrationException.java
│   │
│   ├── repository/                   # Data Access Layer"""

content = content.replace(old_tree_2, new_tree_2)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

print("Done")
