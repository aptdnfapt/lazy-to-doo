# Voice Todo App - Implementation Plan

## Overview
Based on user feedback and codebase analysis, here's the refined implementation plan for fixing critical issues.

## Key Insights from User Feedback

### 1. Category Filtering Strategy
- **Keep Category ID parameter** - Essential for AI to understand context
- **Implement permission-based filtering** - If user selects "Study" category, AI should only work within that category
- **Add validation logic** - If AI tries to access restricted categories, show helpful error messages

### 2. Auto-Approved Tool Call Display
- **Reuse existing ToolCallBubble** - Same blue bubble UI for both manual and auto-approved tools
- **Bypass approval UI** - Skip the approval dialog but show the tool execution and results
- **Maintain consistency** - Keep the same visual language and interaction patterns

### 3. Category Dropdown Issues
- **Fix flickering/glitchy behavior** - Improve dropdown stability
- **Better UI formatting** - Ensure proper integration with overall design

## Detailed Implementation Plan

### Phase 1: Critical UI Fixes (Priority 1)

#### 1.1 Fix Auto-Approved Tool Call Display
**Files to Modify:**
- `app/src/main/java/com/yourname/voicetodo/ai/tools/TodoTools.kt`
- `app/src/main/java/com/yourname/voicetodo/ui/screens/chat/ChatViewModel.kt`

**Changes:**
```kotlin
// In TodoTools.kt - Modify executeWithPermission function (lines 32-38)
// Instead of just notifying completion, ensure tool call messages are created
// for auto-approved tools with EXECUTING status

// In ChatViewModel.kt - Update tool permission handling (lines 106-116)
// Create tool call message for auto-approved tools immediately
// Set status to EXECUTING and autoApproved = true
```

**Expected Result:** Auto-approved tools will show blue tool call bubbles with execution status and results, just like manually approved tools.

#### 1.2 Fix Settings Screen UI Issues
**Files to Modify:**
- `app/src/main/java/com/yourname/voicetodo/ui/screens/settings/SettingsScreen.kt`

**Changes:**
1. **Replace Show/Hide + Set buttons** with single "Edit" button
2. **Implement direct inline editing** for API key fields
3. **Improve padding and spacing** throughout the screen
4. **Use proper icons** instead of "S"/"H" text

```kotlin
// Replace lines 218-236 with cleaner implementation
// Use proper Material Design icons (Visibility/VisibilityOff)
// Add better spacing and consistent button sizing
```

**Expected Result:** Clean, intuitive settings interface with proper padding and fewer confusing buttons.

#### 1.3 Fix Category Dropdown Glitches
**Files to Modify:**
- `app/src/main/java/com/yourname/voicetodo/ui/screens/chat/components/CategoryDropdown.kt`

**Changes:**
1. **Improve dropdown stability** - Fix flickering behavior
2. **Better state management** - Ensure proper expansion/collapse handling
3. **Consistent styling** - Match overall app design language

**Expected Result:** Smooth, reliable category dropdown that works properly with the UI.

### Phase 2: Enhanced Filtering Logic (Priority 2)

#### 2.1 Implement Category Permission System
**Files to Modify:**
- `app/src/main/java/com/yourname/voicetodo/ui/screens/chat/ChatViewModel.kt`
- `app/src/main/java/com/yourname/voicetodo/ai/tools/TodoTools.kt`

**Changes:**
1. **Add strict category validation** in `listTodos` tool
2. **Block unauthorized category access** completely
3. **Return clear error messages** when AI tries to access restricted categories

```kotlin
// In TodoTools.kt listTodos function (lines 472-525)
// Add validation: if selectedCategoryId != null && categoryId != selectedCategoryId && categoryId != "all"
// Return error message: "Access denied: Cannot access category [X]. You are only working with [Y] category."
// NO OPTION TO SWITCH - AI must work within user's selected boundaries
```

**Expected Result:** AI is strictly confined to user's selected category and cannot bypass these boundaries. If user wants to switch categories, they must do it manually via the dropdown.

#### 2.2 Add "Active" Status Support
**Files to Modify:**
- `app/src/main/java/com/yourname/voicetodo/ai/tools/TodoTools.kt`

**Changes:**
1. **Add "active" status handling** in `listTodos` function
2. **Filter for TODO + IN_PROGRESS** when status = "active"
3. **Update status enum** if needed

```kotlin
// In listTodos function, add case for status == "active"
// Return todos where status is TODO or IN_PROGRESS
```

**Expected Result:** Users can see "active" todos (TODO + IN_PROGRESS) excluding completed ones.

### Phase 3: Enhanced Tool Output (Priority 3)

#### 3.1 Improve listTodos Tool Output
**Files to Modify:**
- `app/src/main/java/com/yourname/voicetodo/ai/tools/TodoTools.kt`

**Changes:**
1. **Add description field** to tool output
2. **Include subtasks** in the formatted output
3. **Better formatting** for AI comprehension

```kotlin
// In listTodos function (lines 506-517)
// Add description field: "Description: ${todo.description}"
// Include subtasks: "Subtasks: ${todo.subtasks.size} items"
```

**Expected Result:** AI gets more context about todos, leading to better understanding and responses.

#### 3.2 Fix Todo Details Title Overflow
**Files to Modify:**
- `app/src/main/java/com/yourname/voicetodo/ui/screens/todos/TodoDetailsScreen.kt`

**Changes:**
1. **Add text overflow handling** for long titles
2. **Implement proper truncation** with ellipsis
3. **Consider multi-line support** if needed

```kotlin
// In TodoDetailsScreen.kt line 70
// Change: Text(todo?.title ?: "Task Details")
// To: Text(todo?.title ?: "Task Details", maxLines = 2, overflow = TextOverflow.Ellipsis)
```

**Expected Result:** Long todo titles display properly without breaking the UI.

## Implementation Order

### Week 1: Phase 1 Implementation
- [ ] Fix auto-approved tool call display
- [ ] Fix settings screen UI issues  
- [ ] Fix category dropdown glitches

### Week 2: Phase 2 Implementation
- [ ] Implement strict category permission system (AI cannot bypass user's category selection)
- [ ] Add "active" status support

### Week 3: Phase 3 Implementation
- [ ] Improve listTodos tool output
- [ ] Fix todo details title overflow

## Success Criteria

### UI Consistency
- All tool calls (manual and auto-approved) show blue bubbles
- Settings screen has consistent padding and intuitive controls
- Category dropdown works smoothly without glitches

### User Experience
- Users can see what AI agents do with auto-approved tools
- AI is strictly confined to user's selected category (no unauthorized access)
- Clear feedback when AI tries to access restricted categories: "Access denied: Cannot access category [X]. You are only working with [Y] category."
- User must manually switch categories via dropdown - AI cannot override this
- No UI overflow issues with long text

### AI Agent Behavior
- Respects category boundaries set by user
- Provides helpful error messages for permission issues
- Gets better context from enhanced tool outputs

## Technical Notes

### Backward Compatibility
- Keep existing API key handling for migration
- Ensure category ID parameter continues to work
- Maintain existing tool call interfaces

### Error Handling
- Add proper error messages for category access violations
- Handle edge cases in dropdown state management
- Ensure graceful degradation for invalid inputs

### Testing Considerations
- Test auto-approved tool calls with various tools
- Verify category filtering works correctly
- Test UI with long titles and descriptions