import Spinner from "./Spinner";

const LoadingScreen = ({ message = "Đang tải..." }) => {
    return (
        <div className="flex items-center justify-center min-h-[50vh] flex-col gap-4 ">
            <Spinner size="xl" className="text-primary-600" />
            <p className="text-char-600 font-medium">{message}</p>
        </div>
    );
};

export default LoadingScreen;
