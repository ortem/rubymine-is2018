# always true
if x <= 15 or x >= 15:
    pass

# always false
if x <= 5 and x > 5:
    pass

# always true
if x + 10 <= 25 or x >= 3 * 5:
    pass

# always true
if x - 1 <= 1 * 3 or x >= 2 * 3 or (x >= 4 and x <= 6):
    pass

# always true
if not (x > 10 and x < 20) or (x > 10):
    pass

# always true
if -x + 1 > 5 or x > -2 * 5:
    pass

# always true
if 1 - x > 5 or x - 1 > -2 * 5:
    pass
