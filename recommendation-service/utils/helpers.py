def truncate_uuid(uuid_str: str, length: int = 8) -> str:
    """Rút gọn UUID cho logging."""
    return uuid_str[:length] + "..." if len(uuid_str) > length else uuid_str
